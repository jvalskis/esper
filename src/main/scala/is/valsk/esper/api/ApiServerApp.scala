package is.valsk.esper.api

import is.valsk.esper.EsperConfig
import is.valsk.esper.device.DeviceDescriptor
import is.valsk.esper.model.Device.encoder
import is.valsk.esper.services.{DeviceRepository, FirmwareDownloader, InMemoryDeviceRepository}
import zio.http.*
import zio.http.model.{Method, Status}
import zio.http.netty.NettyServerConfig
import zio.json.*
import zio.{Random, Task, ZIO, ZLayer}

class ApiServerApp(
    deviceRepository: DeviceRepository,
    firmwareDownloader: FirmwareDownloader,
    esperConfig: EsperConfig,
) {

  private val zApp: HttpApp[Any, Response] = Http.collectZIO[Request] {
    case Method.GET -> !! / "devices" =>
      for {
        deviceList <- deviceRepository.list
        response <- ZIO.succeed(Response.json(deviceList.toJson))
      } yield response

    case Method.GET -> !! / "devices" / deviceId => for {
      device <- deviceRepository.get(deviceId)
      response = device match {
        case Some(value) => Response.json(value.toJson)
        case None => Response.status(Status.NotFound)
      }
    } yield response

    case Method.GET -> !! / "firmware" / manufacturer / model => for {
      _ <- firmwareDownloader.downloadFirmware(DeviceDescriptor(manufacturer, None, model))
        .mapError(_ => Response.status(Status.BadRequest))
    } yield Response.status(Status.Ok)
  }

  def run: Task[Nothing] =
    val serverConfigLayer = ServerConfig.live(
      ServerConfig.default.port(esperConfig.port)
    )
    val httpServer = Server.install(zApp).flatMap { port =>
      ZIO.logInfo(s"Starting server on http://localhost:$port")
    }
    (httpServer *> ZIO.never).provide(
      serverConfigLayer,
      NettyServerConfig.live,
      Server.customized,
    )
}

object ApiServerApp {

  val layer: ZLayer[DeviceRepository & EsperConfig & FirmwareDownloader, Throwable, ApiServerApp] = ZLayer {
    for {
      esperConfig <- ZIO.service[EsperConfig]
      deviceRepository <- ZIO.service[DeviceRepository]
      firmwareDownloader <- ZIO.service[FirmwareDownloader]
    } yield ApiServerApp(deviceRepository, firmwareDownloader, esperConfig)
  }
}