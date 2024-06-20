package is.valsk.esper.api.devices.endpoints

import is.valsk.esper.domain.{Device, PersistenceException}
import is.valsk.esper.repositories.DeviceRepository
import zio.{IO, URLayer, ZLayer}

class ListDevices(
    deviceRepository: DeviceRepository
) {

  def apply(): IO[PersistenceException, List[Device]] =
    deviceRepository.getAll
}

object ListDevices {

  val layer: URLayer[DeviceRepository, ListDevices] = ZLayer.fromFunction(ListDevices(_))

}

