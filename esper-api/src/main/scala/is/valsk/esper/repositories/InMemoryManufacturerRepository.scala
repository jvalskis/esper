package is.valsk.esper.repositories

import is.valsk.esper.device.DeviceManufacturerHandler
import is.valsk.esper.domain.PersistenceException
import is.valsk.esper.domain.Types.Manufacturer
import is.valsk.esper.hass.HassToDomainMapper
import zio.*

class InMemoryManufacturerRepository(map: Ref[Map[Manufacturer, DeviceManufacturerHandler with HassToDomainMapper]]) extends ManufacturerRepository {

  override def get(manufacturer: Manufacturer): UIO[DeviceManufacturerHandler with HassToDomainMapper] = map.get.map(_(manufacturer))

  override def getOpt(manufacturer: Manufacturer): UIO[Option[DeviceManufacturerHandler with HassToDomainMapper]] = map.get.map(_.get(manufacturer))

  override def getAll: UIO[List[DeviceManufacturerHandler with HassToDomainMapper]] = map.get.map(_.values.toList)

  override def add(handler: DeviceManufacturerHandler with HassToDomainMapper): UIO[DeviceManufacturerHandler with HassToDomainMapper] = ???

  override def update(value: DeviceManufacturerHandler with HassToDomainMapper): IO[PersistenceException, DeviceManufacturerHandler with HassToDomainMapper] = ???
}

object InMemoryManufacturerRepository {

  val layer: URLayer[Map[Manufacturer, DeviceManufacturerHandler with HassToDomainMapper], ManufacturerRepository] = ZLayer {
    for {
      manufacturerHandlerMap <- ZIO.service[Map[Manufacturer, DeviceManufacturerHandler with HassToDomainMapper]]
      ref <- Ref.make(manufacturerHandlerMap)
    } yield InMemoryManufacturerRepository(ref)
  }
}