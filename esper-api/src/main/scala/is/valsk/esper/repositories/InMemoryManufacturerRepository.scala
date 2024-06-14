package is.valsk.esper.repositories

import is.valsk.esper.device.DeviceHandler
import is.valsk.esper.domain.PersistenceException
import is.valsk.esper.domain.Types.Manufacturer
import is.valsk.esper.hass.HassToDomainMapper
import zio.*

class InMemoryManufacturerRepository(map: Ref[Map[Manufacturer, DeviceHandler]]) extends ManufacturerRepository {

  override def get(manufacturer: Manufacturer): UIO[DeviceHandler] = map.get.map(_(manufacturer))

  override def getOpt(manufacturer: Manufacturer): UIO[Option[DeviceHandler]] = map.get.map(_.get(manufacturer))

  override def getAll: UIO[List[DeviceHandler]] = map.get.map(_.values.toList)

  override def add(handler: DeviceHandler): UIO[DeviceHandler] = ???

  override def update(value: DeviceHandler): IO[PersistenceException, DeviceHandler] = ???
}

object InMemoryManufacturerRepository {

  val layer: URLayer[Map[Manufacturer, DeviceHandler], ManufacturerRepository] = ZLayer {
    for {
      manufacturerHandlerMap <- ZIO.service[Map[Manufacturer, DeviceHandler]]
      ref <- Ref.make(manufacturerHandlerMap)
    } yield InMemoryManufacturerRepository(ref)
  }
}