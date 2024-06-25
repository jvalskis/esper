package is.valsk.esper.api

import is.valsk.esper.config.HttpServerConfig
import sttp.tapir.server.interceptor.cors.CORSInterceptor
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.http.*
import zio.{RLayer, Task, TaskLayer, ZIO, ZLayer}

trait ApiServerApp {
  def run: Task[Unit]
}

object ApiServerApp {
  private class ApiServerAppLive(
      httpApi: HttpApi,
  ) extends ApiServerApp {

    def run: Task[Unit] = serverProgram.provide(
      configuredServerLayer
    )

    private val serverProgram = for {
      endpoints <- httpApi.endpointsZIO
      _ <- ZIO.logInfo("Starting the server...")
      _ <- Server.install(
        ZioHttpInterpreter(
          ZioHttpServerOptions.default.appendInterceptor(
            CORSInterceptor.default
          )
        ).toHttp(endpoints) @@ Middleware.debug
      )
      _ <- ZIO.service[Server.Config]
        .flatMap(config => ZIO.logInfo(s"Server started. Listening on address '${config.address}'"))
        .provide(configuredServerConfigLayer)
      result <- ZIO.never
    } yield result
  }

  private val configuredServerConfigLayer: TaskLayer[Server.Config] = HttpServerConfig.layer >>> ZLayer {
    ZIO.service[HttpServerConfig].map(config => Server.Config.default.binding(config.host, config.port))
  }

  private val configuredServerLayer: TaskLayer[Server] =
    configuredServerConfigLayer >>> Server.live

  val configuredLayer: RLayer[HttpApi, ApiServerApp] = ZLayer.fromFunction(ApiServerAppLive(_))
}