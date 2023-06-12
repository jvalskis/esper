package is.valsk.esper.services

import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import zio.{IO, URLayer, ZIO, ZLayer}

class FirmwareService(
    firmwareRepository: FirmwareRepository,
    manufacturerRepository: ManufacturerRepository,
) {

  def getFirmware(manufacturer: Manufacturer, model: Model, version: Version): IO[EsperError, Firmware] = {
    firmwareRepository
      .get(FirmwareKey(manufacturer, model, version))
      .mapError {
        case EmptyResult() => FirmwareNotFound("Firmware not found", manufacturer, model, Some(version))
      }
  }

  def getLatestFirmware(manufacturer: Manufacturer, model: Model): IO[EsperError, Firmware] = for {
    manufacturerHandler <- manufacturerRepository.get(manufacturer)
    latestFirmware <- firmwareRepository.getLatestFirmware(manufacturer, model)(using manufacturerHandler.versionOrdering)
      .flatMap {
        case None => ZIO.fail(FirmwareNotFound("Latest firmware not found", manufacturer, model, None))
        case Some(result) => ZIO.succeed(result)
      }
  } yield latestFirmware
}

object FirmwareService {
  val layer: URLayer[FirmwareRepository & ManufacturerRepository, FirmwareService] = ZLayer.fromFunction(FirmwareService(_, _))
}
