package is.valsk.esper.api.devices.endpoints

import is.valsk.esper.domain.{PendingUpdate, PersistenceException}
import is.valsk.esper.repositories.PendingUpdateRepository
import zio.{IO, URLayer, ZLayer}

class GetPendingUpdates(
    pendingUpdateRepository: PendingUpdateRepository
) {

  def apply(): IO[PersistenceException, List[PendingUpdate]] =
    pendingUpdateRepository.getAll
}

object GetPendingUpdates {

  val layer: URLayer[PendingUpdateRepository, GetPendingUpdates] = ZLayer.fromFunction(GetPendingUpdates(_))

}

