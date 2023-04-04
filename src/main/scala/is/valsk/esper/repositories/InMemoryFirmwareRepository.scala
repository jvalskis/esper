package is.valsk.esper.repositories

import is.valsk.esper.device.DeviceDescriptor
import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.hass.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.types.Manufacturer
import zio.*

class InMemoryFirmwareRepository(map: Ref[Map[DeviceDescriptor, FirmwareDescriptor]]) extends FirmwareRepository {

  override def get(id: DeviceDescriptor): UIO[Option[FirmwareDescriptor]] = map.get.map(_.get(id))

  override def list: UIO[List[FirmwareDescriptor]] = map.get.map(_.values.toList)

  override def add(item: FirmwareDescriptor): UIO[Unit] = map.update(map => map + (item.deviceDescriptor -> item))
}

object InMemoryFirmwareRepository {

  val layer: ULayer[FirmwareRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[DeviceDescriptor, FirmwareDescriptor])
    } yield InMemoryFirmwareRepository(ref)
  }
}