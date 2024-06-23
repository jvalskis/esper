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
      _ <- Server.serve(
        ZioHttpInterpreter(
          ZioHttpServerOptions.default.appendInterceptor(
            CORSInterceptor.default
          )
        ).toHttp(endpoints) @@ Middleware.debug
      )
    } yield ()
  }

  private val serverConfig: RLayer[HttpServerConfig, Server.Config] = ZLayer {
    ZIO.service[HttpServerConfig].map(config => Server.Config.default.binding(config.host, config.port))
  }

  private val configuredServerLayer: TaskLayer[Server] =
    HttpServerConfig.layer >>> serverConfig >>> Server.live

  val configuredLayer: RLayer[HttpApi, ApiServerApp] = ZLayer.fromFunction(ApiServerAppLive(_))
}