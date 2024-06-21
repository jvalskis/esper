package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.domain.{EsperError, Version}
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import zio.{IO, URLayer, ZLayer}

class ListFirmwareVersions(
    firmwareRepository: FirmwareRepository,
    manufacturerRepository: ManufacturerRepository,
) {

  def apply(manufacturer: Manufacturer, model: Model): IO[EsperError, List[Version]] = for {
    manufacturerHandler <- manufacturerRepository.get(manufacturer)
    versions <- firmwareRepository.listVersions(manufacturer, model)(using manufacturerHandler.versionOrdering)
  } yield versions
}

object ListFirmwareVersions {

  val layer: URLayer[FirmwareRepository & ManufacturerRepository, ListFirmwareVersions] = ZLayer.fromFunction(ListFirmwareVersions(_, _))
}