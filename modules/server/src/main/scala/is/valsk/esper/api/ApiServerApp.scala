package is.valsk.esper.api

import is.valsk.esper.config.HttpServerConfig
import sttp.tapir.server.ziohttp.{ZioHttpInterpreter, ZioHttpServerOptions}
import zio.http.*
import zio.http.netty.NettyConfig
import zio.{RLayer, Task, ULayer, URLayer, ZIO, ZLayer}

trait ApiServerApp {
  def run: Task[Unit]
}

object ApiServerApp {
  private class ApiServerAppLive(
      httpApi: HttpApi,
      httpServerConfig: HttpServerConfig,
  ) extends ApiServerApp {

    def run: Task[Unit] = serverProgram.provide(
      serverConfig,
      nettyConfig,
      Server.customized,
    )

    private val serverProgram = for {
      endpoints <- httpApi.endpointsZIO
      _ <- Server.serve(
        ZioHttpInterpreter(
          ZioHttpServerOptions.default
        ).toHttp(endpoints) @@ Middleware.debug
      )
    } yield ()

    private val serverConfig: ULayer[Server.Config] =
      ZLayer.succeed(Server.Config.default.binding(httpServerConfig.host, httpServerConfig.port))

    private val nettyConfig: ULayer[NettyConfig] =
      ZLayer.succeed(NettyConfig.default)
  }

  val layer: URLayer[HttpServerConfig & HttpApi, ApiServerApp] = ZLayer.fromFunction(ApiServerAppLive(_, _))

  val configuredLayer: RLayer[HttpApi, ApiServerApp] = HttpServerConfig.layer >>> layer
}