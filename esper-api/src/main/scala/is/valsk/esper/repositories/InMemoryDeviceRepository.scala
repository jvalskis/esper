package is.valsk.esper.repositories

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.domain.Device
import is.valsk.esper.domain.Types.DeviceId
import zio.*

class InMemoryDeviceRepository(map: Ref[Map[DeviceId, Device]]) extends DeviceRepository {

  override def get(id: DeviceId): UIO[Device] = map.get.map(_(id))

  override def getOpt(id: DeviceId): UIO[Option[Device]] = map.get.map(_.get(id))

  override def getAll: UIO[List[Device]] = map.get.map(_.values.toList)

  override def add(device: Device): UIO[Device] = map.update(map => map + (device.id -> device)).as(device)

  override def update(device: Device): UIO[Device] = map.update(map => map + (device.id -> device)).as(device)
}

object InMemoryDeviceRepository {

  val layer: ULayer[DeviceRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[NonEmptyString, Device])
    } yield InMemoryDeviceRepository(ref)
  }
}