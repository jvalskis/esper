package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.domain.Version
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import zio.http.*
import zio.http.model.*
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

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