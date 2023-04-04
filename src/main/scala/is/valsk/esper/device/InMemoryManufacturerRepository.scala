package is.valsk.esper.device

import is.valsk.esper.device.ManufacturerRepository
import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.types.Manufacturer
import zio.*

class InMemoryManufacturerRepository(manufacturerMap: Ref[Map[Manufacturer, DeviceManufacturerHandler]]) extends ManufacturerRepository {

  override def get(manufacturer: Manufacturer): UIO[Option[DeviceManufacturerHandler]] = manufacturerMap.get.map(_.get(manufacturer))

  override def list: UIO[List[DeviceManufacturerHandler]] = manufacturerMap.get.map(_.values.toList)

  override def add(handler: DeviceManufacturerHandler): UIO[Unit] = ???
}

object InMemoryManufacturerRepository {

  val layer: URLayer[Map[Manufacturer, DeviceManufacturerHandler], ManufacturerRepository] = ZLayer {
    for {
      manufacturerHandlerMap <- ZIO.service[Map[Manufacturer, DeviceManufacturerHandler]]
      ref <- Ref.make(manufacturerHandlerMap)
    } yield InMemoryManufacturerRepository(ref)
  }
}