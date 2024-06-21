package is.valsk.esper.api.devices.endpoints

import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.domain.{EntityNotFound, PendingUpdate, PersistenceException}
import is.valsk.esper.repositories.PendingUpdateRepository
import zio.{IO, URLayer, ZLayer}

class GetPendingUpdate(
    pendingUpdateRepository: PendingUpdateRepository
) {

  def apply(deviceId: DeviceId): IO[EntityNotFound | PersistenceException, PendingUpdate] =
    pendingUpdateRepository.get(deviceId)
}

object GetPendingUpdate {

  val layer: URLayer[PendingUpdateRepository, GetPendingUpdate] = ZLayer.fromFunction(GetPendingUpdate(_))

}

