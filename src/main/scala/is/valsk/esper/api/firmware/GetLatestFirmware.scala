package is.valsk.esper.api.firmware

import is.valsk.esper.api.FirmwareApi
import is.valsk.esper.device.DeviceManufacturerHandler
import is.valsk.esper.domain.Types.{Manufacturer, ManufacturerExtractor, Model, ModelExtractor}
import is.valsk.esper.domain.{DeviceModel, PersistenceException, Version}
import is.valsk.esper.hass.HassToDomainMapper
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import is.valsk.esper.services.FirmwareDownloader
import zio.http.*
import zio.http.model.HttpError.NotFound
import zio.http.model.{HeaderValues, Headers, HttpError, Status}
import zio.stream.ZStream
import zio.{Chunk, IO, URLayer, ZIO, ZLayer}

class GetLatestFirmware(
    firmwareRepository: FirmwareRepository,
    manufacturerRepository: ManufacturerRepository,
) {

  def apply(manufacturer: Manufacturer, model: Model): IO[HttpError, Response] = {
    for {
      _ <- ZIO.logInfo(s"Getting latest firmware for manufacturer: $manufacturer, model: $model")
      manufacturerHandler <- manufacturerRepository.get(manufacturer)
        .mapError(_ => NotFound(""))
        .flatMap {
          case Some(handler) => ZIO.succeed(handler)
          case None => ZIO.fail(NotFound(""))
        }
      latestFirmware <- firmwareRepository.getLatestFirmware(manufacturer, model)(using manufacturerHandler.versionOrdering)
        .mapError(_ => NotFound(""))
        .flatMap {
        case None => ZIO.fail(NotFound(""))
        case Some(result) => ZIO.succeed(result)
      }
    } yield Response(
      status = Status.Ok,
      headers = Headers.contentLength(latestFirmware.size) ++ Headers.contentType(HeaderValues.applicationOctetStream),
      body = Body.fromChunk(Chunk.from(latestFirmware.data)),
    )
  }
}

object GetLatestFirmware {

  val layer: URLayer[FirmwareRepository & ManufacturerRepository, GetLatestFirmware] = ZLayer.fromFunction(GetLatestFirmware(_, _))
}