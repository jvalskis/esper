package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.domain.Version
import is.valsk.esper.repositories.FirmwareRepository
import zio.{IO, URLayer, ZLayer}

class DeleteFirmware(
    firmwareRepository: FirmwareRepository,
) {

  def apply(manufacturer: Manufacturer, model: Model, version: Version): IO[Throwable, Unit] = ???
}

object DeleteFirmware {

  val layer: URLayer[FirmwareRepository, DeleteFirmware] = ZLayer.fromFunction(DeleteFirmware(_))
}