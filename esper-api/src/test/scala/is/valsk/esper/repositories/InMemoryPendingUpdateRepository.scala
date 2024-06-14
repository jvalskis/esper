package is.valsk.esper.repositories

import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.model.api.PendingUpdate
import zio.*

class InMemoryPendingUpdateRepository(
    map: Ref[Map[DeviceId, PendingUpdate]]
) extends PendingUpdateRepository {

  override def get(id: DeviceId): UIO[PendingUpdate] = map.get.map(_(id))

  override def getAll: UIO[List[PendingUpdate]] = map.get.map(_.values.toList)

  override def add(pendingUpdate: PendingUpdate): IO[FailedToStoreFirmware, PendingUpdate] = for {
    _ <- map.update(map => map + (pendingUpdate.device.id -> pendingUpdate))
  } yield pendingUpdate

  override def update(pendingUpdate: PendingUpdate): IO[FailedToStoreFirmware, PendingUpdate] = for {
    _ <- map.update(map => map + (pendingUpdate.device.id -> pendingUpdate))
  } yield pendingUpdate

  override def getOpt(id: DeviceId): UIO[Option[PendingUpdate]] = map.get.map(_.get(id))

}

object InMemoryPendingUpdateRepository {

  val layer: ULayer[PendingUpdateRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[DeviceId, PendingUpdate])
    } yield InMemoryPendingUpdateRepository(ref)
  }
}