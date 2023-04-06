package is.valsk.esper.api

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.EsperConfig
import is.valsk.esper.device.DeviceProxy
import is.valsk.esper.device.shelly.ShellyDeviceHandler.ShellyDevice
import is.valsk.esper.domain.Device.encoder
import is.valsk.esper.domain.SemanticVersion.encoder
import is.valsk.esper.domain.{DeviceModel, SemanticVersion}
import is.valsk.esper.repositories.{DeviceRepository, InMemoryDeviceRepository, Repository}
import is.valsk.esper.services.FirmwareDownloader
import zio.http.*
import zio.http.model.{Method, Status}
import zio.http.netty.NettyServerConfig
import zio.json.*
import zio.{RLayer, Random, Task, URLayer, ZIO, ZLayer}

class ApiServerApp(
    firmwareApi: FirmwareApi,
    deviceApi: DeviceApi,
    esperConfig: EsperConfig,
) {

  def run: Task[Nothing] =
    val serverConfigLayer = ServerConfig.live(
      ServerConfig.default.port(esperConfig.port)
    )
    val httpServer = Server.install(firmwareApi.app ++ deviceApi.аpp).flatMap { port =>
      ZIO.logInfo(s"Starting server on http://localhost:$port")
    }
    (httpServer *> ZIO.never).provide(
      serverConfigLayer,
      NettyServerConfig.live,
      Server.customized,
    )
}

object ApiServerApp {

  val layer: URLayer[DeviceApi & FirmwareApi & EsperConfig, ApiServerApp] = ZLayer {
    for {
      esperConfig <- ZIO.service[EsperConfig]
      deviceApi <- ZIO.service[DeviceApi]
      firmwareApi <- ZIO.service[FirmwareApi]
    } yield ApiServerApp(firmwareApi, deviceApi, esperConfig)
  }
}