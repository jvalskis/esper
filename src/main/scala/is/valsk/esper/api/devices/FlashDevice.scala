package is.valsk.esper.api.devices

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.{DeviceProxy, DeviceProxyRegistry}
import is.valsk.esper.domain.{DeviceModel, Version}
import is.valsk.esper.domain.Version.encoder
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.{DeviceRepository, FirmwareRepository}
import zio.http.Response
import zio.http.model.{HttpError, Status}
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class FlashDevice(
    deviceProxyRegistry: DeviceProxyRegistry,
    deviceRepository: DeviceRepository,
    firmwareRepository: FirmwareRepository
) {

  def apply(deviceId: NonEmptyString, version: Version): IO[HttpError, Response] = for {
    device <- deviceRepository.get(deviceId)
      .flatMap {
        case Some(value) => ZIO.succeed(value)
        case None =>
          ZIO.fail(HttpError.NotFound("1"))
      }
      .mapError(_ => HttpError.InternalServerError())
    firmware <- firmwareRepository.get(FirmwareKey(device.manufacturer, device.model, version))
      .mapError(_ => HttpError.InternalServerError())
      .flatMap {
        case Some(value) => ZIO.succeed(value)
        case None =>
          ZIO.fail(HttpError.NotFound("2"))
      }
    deviceProxy <- deviceProxyRegistry.selectProxy(device.manufacturer)
      .mapError(e => HttpError.PreconditionFailed(e.getMessage))
    _ <- deviceProxy.flashFirmware(device, firmware)
      .mapError(e => HttpError.BadGateway(e.getMessage))
  } yield Response.ok
}

object FlashDevice {

  val layer: URLayer[DeviceProxyRegistry & DeviceRepository & FirmwareRepository, FlashDevice] = ZLayer.fromFunction(FlashDevice(_, _, _))

}
