package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.domain.{FirmwareNotFound, ManufacturerNotSupported, PersistenceException, Version}
import is.valsk.esper.services.FirmwareService
import zio.http.*
import zio.http.model.*
import zio.http.model.HttpError.NotFound
import zio.{Chunk, IO, URLayer, ZLayer}

class GetFirmware(
    firmwareService: FirmwareService,
) {

  def apply(manufacturer: Manufacturer, model: Model, maybeVersion: Option[Version] = None): IO[HttpError, Response] = {
    for {
      firmware <- maybeVersion match
        case Some(version) =>
          firmwareService.getFirmware(manufacturer, model, version)
        case None =>
          firmwareService.getLatestFirmware(manufacturer, model)
    } yield Response(
      status = Status.Ok,
      headers = Headers.contentLength(firmware.size) ++ Headers.contentType("application/zip"),
      body = Body.fromChunk(Chunk.from(firmware.data)),
    )
  }
    .mapError {
      case _: FirmwareNotFound => NotFound("") // TODO error handling
      case e: ManufacturerNotSupported => HttpError.PreconditionFailed(e.getMessage)
      case e => HttpError.InternalServerError(e.getMessage)
    }
}

object GetFirmware {

  val layer: URLayer[FirmwareService, GetFirmware] = ZLayer.fromFunction(GetFirmware(_))
}