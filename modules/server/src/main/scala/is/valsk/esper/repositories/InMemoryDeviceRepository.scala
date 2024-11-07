package is.valsk.esper.repositories

import is.valsk.esper.domain.Device
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.event.*
import zio.*

class InMemoryDeviceRepository(
    map: Ref[Map[DeviceId, Device]],
    deviceEventProducer: DeviceEventProducer,
) extends DeviceRepository {

  override def getOpt(id: DeviceId): UIO[Option[Device]] = map.get.map(_.get(id))

  override def getAll: UIO[List[Device]] = map.get.map(_.values.toList)

  override def add(device: Device): UIO[Device] = for {
    result <- map.update(map => map + (device.id -> device)).as(device)
    _ <- deviceEventProducer.produceEvent(DeviceAdded(result))
  } yield result

  override def update(device: Device): UIO[Device] = for {
    result <- map.update(map => map + (device.id -> device)).as(device)
    _ <- deviceEventProducer.produceEvent(DeviceUpdated(result))
  } yield result

  override def delete(id: DeviceId): UIO[Unit] = for {
    deviceToRemove <- map.get.map(_.get(id))
    result <- map.update(map => map - id)
    _ <- deviceToRemove.fold(ZIO.unit)(device => deviceEventProducer.produceEvent(DeviceRemoved(device)))
  } yield result
}

object InMemoryDeviceRepository {

  val layer: RLayer[DeviceEventProducer, DeviceRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[DeviceId, Device])
      eventProducer <- ZIO.service[DeviceEventProducer]
    } yield new InMemoryDeviceRepository(ref, eventProducer)
  }
}