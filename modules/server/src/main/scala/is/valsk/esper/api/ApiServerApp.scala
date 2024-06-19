package is.valsk.esper.api

import is.valsk.esper.api.devices.DeviceApi
import is.valsk.esper.api.firmware.FirmwareApi
import is.valsk.esper.api.ota.OtaApi
import is.valsk.esper.config.RestServerConfig
import zio.http.*
import zio.http.middleware.RequestHandlerMiddlewares
import zio.http.netty.NettyServerConfig
import zio.{RLayer, Task, URLayer, ZIO, ZLayer}

trait ApiServerApp {
  def run: Task[Nothing]
}

object ApiServerApp {
  private class ApiServerAppLive(
      firmwareApi: FirmwareApi,
      otaApi: OtaApi,
      deviceApi: DeviceApi,
      restServerConfig: RestServerConfig,
  ) extends ApiServerApp {

    def run: Task[Nothing] =
      val serverConfigLayer = ServerConfig.live(
        ServerConfig.default.port(restServerConfig.port)
      )
      val app = (firmwareApi.app ++ deviceApi.app ++ otaApi.app)
        .mapError(e => Response(status = e.status, body = Body.fromCharSequence(e.message))) @@ RequestHandlerMiddlewares.debug
      val httpServer = Server.install(app)
        .flatMap { port =>
          ZIO.logInfo(s"Starting server on http://localhost:$port")
        }
      (httpServer *> ZIO.never).provide(
        serverConfigLayer,
        NettyServerConfig.live,
        Server.customized,
      )
  }

  val layer: URLayer[OtaApi & DeviceApi & FirmwareApi & RestServerConfig, ApiServerApp] = ZLayer.fromFunction(ApiServerAppLive(_, _, _, _))

  val configuredLayer: RLayer[OtaApi & DeviceApi & FirmwareApi, ApiServerApp] = RestServerConfig.layer >>> layer
}