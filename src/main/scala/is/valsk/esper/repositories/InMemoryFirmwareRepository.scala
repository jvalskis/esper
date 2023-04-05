package is.valsk.esper.repositories

import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.hass.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.DeviceModel
import is.valsk.esper.domain.Types.Manufacturer
import zio.*

class InMemoryFirmwareRepository(map: Ref[Map[DeviceModel, FirmwareDescriptor]]) extends FirmwareRepository {

  override def get(id: DeviceModel): UIO[Option[FirmwareDescriptor]] = map.get.map(_.get(id))

  override def list: UIO[List[FirmwareDescriptor]] = map.get.map(_.values.toList)

  override def add(item: FirmwareDescriptor): UIO[Unit] = map.update(map => map + (item.deviceModel -> item))
}

object InMemoryFirmwareRepository {

  val layer: ULayer[FirmwareRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[DeviceModel, FirmwareDescriptor])
    } yield InMemoryFirmwareRepository(ref)
  }
}