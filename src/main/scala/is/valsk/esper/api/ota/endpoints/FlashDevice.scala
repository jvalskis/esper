package is.valsk.esper.api.ota.endpoints

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.{DeviceProxy, DeviceProxyRegistry}
import is.valsk.esper.domain.Version.encoder
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.{DeviceRepository, FirmwareRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareService, OtaService}
import zio.http.Response
import zio.http.model.HttpError.NotFound
import zio.http.model.{HttpError, Status}
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class FlashDevice(
    otaService: OtaService,
    deviceRepository: DeviceRepository,
    firmwareService: FirmwareService,
) {

  def apply(deviceId: DeviceId, maybeVersion: Option[Version]): IO[HttpError, Response] = {
    for {
      device <- deviceRepository.get(deviceId)
      firmware <- maybeVersion match
        case Some(version) =>
          firmwareService.getFirmware(device.manufacturer, device.model, version)
        case None =>
          firmwareService.getLatestFirmware(device.manufacturer, device.model)
      _ <- otaService.flashFirmware(device, firmware)
    } yield Response.ok
  }
    .mapError {
      case _: FirmwareNotFound => NotFound("") // TODO error handling
      case e@MalformedVersion(version, device) => HttpError.BadRequest(e.getMessage)
      case e@ApiCallFailed(message, device, cause) => HttpError.BadGateway(e.getMessage)
      case e@ManufacturerNotSupported(manufacturer) => HttpError.PreconditionFailed(e.getMessage)
      case e@FailedToParseApiResponse(message, device, cause) => HttpError.BadGateway(e.getMessage)
    }
}

object FlashDevice {

  val layer: URLayer[DeviceRepository & OtaService & FirmwareService, FlashDevice] = ZLayer.fromFunction(FlashDevice(_, _, _))

}
