package is.valsk.esper.api.devices.endpoints

import is.valsk.esper.domain.{Device, EntityNotFound, PersistenceException}
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.repositories.DeviceRepository
import zio.{IO, URLayer, ZLayer}

class GetDevice(
    deviceRepository: DeviceRepository
) {

  def apply(deviceId: DeviceId): IO[EntityNotFound | PersistenceException, Device] =
    deviceRepository.get(deviceId)
}

object GetDevice {

  val layer: URLayer[DeviceRepository, GetDevice] = ZLayer.fromFunction(GetDevice(_))

}
