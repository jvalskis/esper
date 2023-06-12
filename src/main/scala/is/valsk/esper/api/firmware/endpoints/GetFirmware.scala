package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, ManufacturerExtractor, Model, ModelExtractor}
import is.valsk.esper.domain.{DeviceModel, PersistenceException, Version}
import is.valsk.esper.repositories.FirmwareRepository
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.services.FirmwareDownloader
import zio.http.*
import zio.http.model.*
import zio.http.model.HttpError.NotFound
import zio.stream.ZStream
import zio.{Chunk, IO, URLayer, ZIO, ZLayer}

class GetFirmware(
    firmwareRepository: FirmwareRepository,
) {

  def apply(manufacturer: Manufacturer, model: Model, version: Version): IO[HttpError, Response] = for {
    _ <- ZIO.logInfo("GetFirmware")
    firmware <- firmwareRepository.get(FirmwareKey(manufacturer, model, version))
      .logError("Failed to get firmware")
      .mapError(_ => HttpError.InternalServerError())// TODO ERROR HANDLING
  } yield Response(
    status = Status.Ok,
    headers = Headers.contentLength(firmware.size) ++ Headers.contentType(HeaderValues.applicationOctetStream),
    body = Body.fromChunk(Chunk.from(firmware.data)),
  )
}

object GetFirmware {

  val layer: URLayer[FirmwareRepository, GetFirmware] = ZLayer.fromFunction(GetFirmware(_))
}