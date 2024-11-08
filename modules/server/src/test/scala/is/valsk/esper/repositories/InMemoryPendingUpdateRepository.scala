package is.valsk.esper.repositories

import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.DeviceId
import zio.*

class InMemoryPendingUpdateRepository(
    map: Ref[Map[DeviceId, PendingUpdate]]
) extends PendingUpdateRepository {

  override def getAll: UIO[List[PendingUpdate]] = map.get.map(_.values.toList)

  override def add(pendingUpdate: PendingUpdate): IO[PersistenceException, PendingUpdate] =
    map.update(map => map + (pendingUpdate.device.id -> pendingUpdate)).as(pendingUpdate)
    
  override def update(pendingUpdate: PendingUpdate): IO[PersistenceException, PendingUpdate] = 
    map.update(map => map + (pendingUpdate.device.id -> pendingUpdate)).as(pendingUpdate)  
    
  override def delete(id: DeviceId): IO[PersistenceException, Unit] =
    map.update(map => map - id).unit
    
  override def find(id: DeviceId): UIO[Option[PendingUpdate]] = map.get.map(_.get(id))

}

object InMemoryPendingUpdateRepository {

  val layer: ULayer[PendingUpdateRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[DeviceId, PendingUpdate])
    } yield InMemoryPendingUpdateRepository(ref)
  }
}