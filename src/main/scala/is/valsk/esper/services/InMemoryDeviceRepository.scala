package is.valsk.esper.services

import is.valsk.esper.model.Device
import zio.*

class InMemoryDeviceRepository extends DeviceRepository {
  private val deviceMap: UIO[Ref[Map[String, Device]]] = Ref.make(Map.empty)

  override def get(id: String): UIO[Option[Device]] = for {
    deviceMapRef <- deviceMap
    maybeDevice <- deviceMapRef.get.map(_.get(id))
  } yield maybeDevice

  override def list: UIO[List[Device]] = for {
    deviceMapRef <- deviceMap
    maybeDevice <- deviceMapRef.get.map(_.values.toList)
  } yield maybeDevice

  override def add(device: Device): UIO[Unit] = for {
    deviceMapRef <- deviceMap
    _ <- deviceMapRef.update(_ + (device.id -> device))
  } yield ()
}

object InMemoryDeviceRepository {

  val layer: ULayer[DeviceRepository] = ZLayer.succeed(InMemoryDeviceRepository())
}