package is.valsk.esper.repositories

import is.valsk.esper.device.DeviceHandler
import is.valsk.esper.domain.PersistenceException
import is.valsk.esper.domain.Types.Manufacturer
import zio.*

class InMemoryManufacturerRepository(map: Ref[Map[Manufacturer, DeviceHandler]]) extends ManufacturerRepository {

  override def getOpt(manufacturer: Manufacturer): UIO[Option[DeviceHandler]] = map.get.map(_.get(manufacturer))

  override def getAll: UIO[List[DeviceHandler]] = map.get.map(_.values.toList)

  override def add(value: DeviceHandler): UIO[DeviceHandler] = map.update(map => map + (value.supportedManufacturer -> value)).as(value)

  override def update(value: DeviceHandler): IO[PersistenceException, DeviceHandler] = map.update(map => map + (value.supportedManufacturer -> value)).as(value)
}

object InMemoryManufacturerRepository {

  val layer: URLayer[Seq[DeviceHandler], ManufacturerRepository] = ZLayer {
    for {
      deviceHandlers <- ZIO.service[Seq[DeviceHandler]]
      ref <- Ref.make(deviceHandlers.map(handler => handler.supportedManufacturer -> handler).toMap)
    } yield InMemoryManufacturerRepository(ref)
  }
}