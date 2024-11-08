package is.valsk.esper.ctx

import is.valsk.esper.domain
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.*
import is.valsk.esper.repositories.*
import zio.*

trait PendingUpdateCtx {

  def getAllPendingUpdates: ZIO[PendingUpdateRepository, Throwable, Seq[PendingUpdate]] =
    ZIO.service[PendingUpdateRepository].flatMap(_.getAll)

  def findPendingUpdate(id: DeviceId): ZIO[PendingUpdateRepository, Throwable, Option[PendingUpdate]] =
    ZIO.service[PendingUpdateRepository].flatMap(_.find(id))

  def getPendingUpdate(id: DeviceId): ZIO[PendingUpdateRepository, Throwable, PendingUpdate] =
    ZIO.service[PendingUpdateRepository].flatMap(_.get(id))

  def givenPendingUpdates(pendingUpdates: PendingUpdate*): URIO[PendingUpdateRepository, Unit] = for {
    pendingUpdateRepository <- ZIO.service[PendingUpdateRepository]
    _ <- ZIO.foreach(pendingUpdates)(pendingUpdateRepository.add).orDie
  } yield ()
}
