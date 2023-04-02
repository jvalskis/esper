package is.valsk.esper.services

import is.valsk.esper.model.Device
import zio.*

class InMemoryDeviceRepository(deviceMap: Ref[Map[String, Device]]) extends DeviceRepository {

  override def get(id: String): UIO[Option[Device]] = deviceMap.get.map(_.get(id))
  override def list: UIO[List[Device]] = deviceMap.get.map(_.values.toList)
  override def add(device: Device): UIO[Unit] = deviceMap.update(map => map + (device.id -> device))}

object InMemoryDeviceRepository {

  val layer: ULayer[DeviceRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty)
    } yield InMemoryDeviceRepository(ref)
  }
}