package is.valsk.esper.api

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.EsperConfig
import is.valsk.esper.api.devices.DeviceApi
import is.valsk.esper.api.firmware.FirmwareApi
import is.valsk.esper.api.ota.OtaApi
import is.valsk.esper.device.DeviceProxy
import is.valsk.esper.domain.Device.encoder
import is.valsk.esper.domain.Version.encoder
import is.valsk.esper.domain.{DeviceModel, SemanticVersion}
import is.valsk.esper.repositories.{DeviceRepository, InMemoryDeviceRepository, Repository}
import is.valsk.esper.services.FirmwareDownloader
import zio.http.*
import zio.http.middleware.RequestHandlerMiddlewares
import zio.http.model.{Method, Status}
import zio.http.netty.NettyServerConfig
import zio.json.*
import zio.{RLayer, Random, Task, URLayer, ZIO, ZLayer}

trait ApiServerApp {
  def run: Task[Nothing]
}

object ApiServerApp {
  private class ApiServerAppLive(
      firmwareApi: FirmwareApi,
      otaApi: OtaApi,
      deviceApi: DeviceApi,
      esperConfig: EsperConfig,
  ) extends ApiServerApp {

    def run: Task[Nothing] =
      val serverConfigLayer = ServerConfig.live(
        ServerConfig.default.port(esperConfig.port)
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

  val layer: URLayer[OtaApi & DeviceApi & FirmwareApi & EsperConfig, ApiServerApp] = ZLayer.fromFunction(ApiServerAppLive(_, _, _, _))
}