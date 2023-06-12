package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, ManufacturerExtractor, Model, ModelExtractor}
import is.valsk.esper.domain.{DeviceModel, FirmwareNotFound, PersistenceException, Version}
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.services.{FirmwareDownloader, FirmwareService}
import zio.http.*
import zio.http.model.*
import zio.http.model.HttpError.NotFound
import zio.stream.ZStream
import zio.{Chunk, IO, URLayer, ZIO, ZLayer}

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
      headers = Headers.contentLength(firmware.size) ++ Headers.contentType(HeaderValues.applicationOctetStream),
      body = Body.fromChunk(Chunk.from(firmware.data)),
    )
  }
    .mapError {
      case _: FirmwareNotFound => NotFound("") // TODO error handling
      case _: PersistenceException => NotFound("") // TODO error handling
      case e => HttpError.InternalServerError(e.getMessage)
    }
}

object GetFirmware {

  val layer: URLayer[FirmwareService, GetFirmware] = ZLayer.fromFunction(GetFirmware(_))
}