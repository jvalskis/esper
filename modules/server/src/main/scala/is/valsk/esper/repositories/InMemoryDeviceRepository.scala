package is.valsk.esper.repositories

import is.valsk.esper.domain.Device
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.event.{DeviceAdded, DeviceEvent, DeviceRemoved, DeviceUpdated}
import zio.*

class InMemoryDeviceRepository(
    map: Ref[Map[DeviceId, Device]],
    deviceEventQueue: Queue[DeviceEvent],
) extends DeviceRepository {

  override def getOpt(id: DeviceId): UIO[Option[Device]] = map.get.map(_.get(id))

  override def getAll: UIO[List[Device]] = map.get.map(_.values.toList)

  override def add(device: Device): UIO[Device] = for {
    result <- map.update(map => map + (device.id -> device)).as(device)
    _ <- deviceEventQueue.offer(DeviceAdded(result))
  } yield result

  override def update(device: Device): UIO[Device] = for {
    result <- map.update(map => map + (device.id -> device)).as(device)
    _ <- deviceEventQueue.offer(DeviceUpdated(result))
  } yield result

  override def delete(id: DeviceId): UIO[Unit] = for {
    deviceToRemove <- map.get.map(_.get(id))
    result <- map.update(map => map - id)
    _ <- ZIO.fromOption(deviceToRemove)
      .map(device => deviceEventQueue.offer(DeviceRemoved(device)))
      .orDie
  } yield result
}

object InMemoryDeviceRepository {

  val layer: RLayer[Queue[DeviceEvent], DeviceRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[DeviceId, Device])
      eventQueue <- ZIO.service[Queue[DeviceEvent]]
    } yield new InMemoryDeviceRepository(ref, eventQueue)
  }
}