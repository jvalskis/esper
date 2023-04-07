package is.valsk.esper.api.devices

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.DeviceProxy
import is.valsk.esper.device.shelly.ShellyDeviceHandler.ShellyDevice
import is.valsk.esper.domain.DeviceModel
import is.valsk.esper.domain.SemanticVersion.encoder
import is.valsk.esper.repositories.{DeviceRepository, FirmwareRepository}
import zio.http.Response
import zio.http.model.{HttpError, Status}
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class FlashDevice(
    deviceProxy: DeviceProxy[ShellyDevice],
    deviceRepository: DeviceRepository,
    firmwareRepository: FirmwareRepository
) {

  def apply(deviceId: NonEmptyString, version: String): IO[HttpError, Response] = for {
    device <- deviceRepository.get(deviceId)
      .flatMap {
        case Some(value) => ZIO.succeed(value)
        case None =>
          ZIO.fail(HttpError.NotFound("1"))
      }
      .mapError(_ => HttpError.InternalServerError())
    firmware <- firmwareRepository.get(DeviceModel(device.manufacturer, device.model))
      .mapError(_ => HttpError.InternalServerError())
      .flatMap {
        case Some(value) => ZIO.succeed(value)
        case None =>
          ZIO.fail(HttpError.NotFound("2"))
      }
    _ <- deviceProxy.flashFirmware(device, firmware)
      .mapError(e => HttpError.BadGateway(e.getMessage))
  } yield Response.ok
}

object FlashDevice {

  val layer: URLayer[DeviceProxy[ShellyDevice] & DeviceRepository & FirmwareRepository, FlashDevice] = ZLayer {
    for {
      deviceProxy <- ZIO.service[DeviceProxy[ShellyDevice]]
      deviceRepository <- ZIO.service[DeviceRepository]
      firmwareRepository <- ZIO.service[FirmwareRepository]
    } yield FlashDevice(deviceProxy, deviceRepository, firmwareRepository)
  }

}
