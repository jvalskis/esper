package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, ManufacturerExtractor, Model, ModelExtractor}
import is.valsk.esper.domain.{DeviceModel, PersistenceException, Version}
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import is.valsk.esper.services.FirmwareDownloader
import zio.http.*
import zio.http.model.*
import zio.http.model.HttpError.NotFound
import zio.json.*
import zio.stream.ZStream
import zio.{Chunk, IO, URLayer, ZIO, ZLayer}

class ListFirmwareVersions(
    firmwareRepository: FirmwareRepository,
    manufacturerRepository: ManufacturerRepository,
) {

  def apply(manufacturer: Manufacturer, model: Model): IO[HttpError, Response] = {
    for {
      _ <- ZIO.logInfo("ListFirmwareVersions")
      manufacturerHandler <- manufacturerRepository.get(manufacturer)
        .logError(s"Failed to get manufacturer: $manufacturer")
      versions <- firmwareRepository.listVersions(manufacturer, model)(using manufacturerHandler.versionOrdering)
        .logError("Failed to list firmware versions")
    } yield Response.json(versions.toJson)
  }
    .mapError {
      case _ => HttpError.InternalServerError()
    }
}

object ListFirmwareVersions {

  val layer: URLayer[FirmwareRepository & ManufacturerRepository, ListFirmwareVersions] = ZLayer.fromFunction(ListFirmwareVersions(_, _))
}