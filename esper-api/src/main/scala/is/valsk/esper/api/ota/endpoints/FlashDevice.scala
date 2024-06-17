package is.valsk.esper.api.ota.endpoints

import is.valsk.esper.device.FlashResult.encoder
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.repositories.DeviceRepository
import is.valsk.esper.services.{FirmwareService, OtaService}
import zio.http.Response
import zio.http.model.HttpError
import zio.http.model.HttpError.NotFound
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class FlashDevice(
    otaService: OtaService,
    deviceRepository: DeviceRepository,
    firmwareService: FirmwareService,
) {

  def apply(deviceId: DeviceId, maybeVersion: Option[Version]): IO[HttpError, Response] = {
    for {
      device <- getDevice(deviceId)
      firmware <- maybeVersion match
        case Some(version) =>
          firmwareService.getFirmware(device.manufacturer, device.model, version)
        case None =>
          firmwareService.getLatestFirmware(device.manufacturer, device.model)
      result <- otaService.flashFirmware(device, firmware)
    } yield Response.json(result.toJson)
  }
    .mapError {
      case e: DeviceNotFound => HttpError.NotFound("")
      case _: FirmwareNotFound => HttpError.NotFound("") // TODO error handling
      case e: MalformedVersion => HttpError.BadRequest(e.getMessage)
      case e: ApiCallFailed => HttpError.BadGateway(e.getMessage)
      case e: ManufacturerNotSupported => HttpError.PreconditionFailed(e.getMessage)
      case e: FailedToParseApiResponse => HttpError.BadGateway(e.getMessage)
      case e => HttpError.InternalServerError(e.getMessage)
    }

  private def getDevice(deviceId: DeviceId): IO[PersistenceException | DeviceNotFound, Device] = for {
    maybeDevice <- deviceRepository.getOpt(deviceId)
    device <- ZIO.fromOption(maybeDevice)
      .mapError(_ => DeviceNotFound(deviceId))
  } yield device
}

object FlashDevice {

  val layer: URLayer[DeviceRepository & OtaService & FirmwareService, FlashDevice] = ZLayer.fromFunction(FlashDevice(_, _, _))

}
