package is.valsk.esper.api.devices.endpoints

import is.valsk.esper.domain.PersistenceException
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.repositories.PendingUpdateRepository
import zio.http.Response
import zio.http.model.HttpError
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class GetPendingUpdate(
    pendingUpdateRepository: PendingUpdateRepository
) {

  def apply(deviceId: DeviceId): IO[HttpError, Response] = {
    for {
      deviceList <- pendingUpdateRepository.get(deviceId)
      response <- ZIO.succeed(Response.json(deviceList.toJson))
    } yield response
  }
    .mapError {
      case _: PersistenceException => HttpError.NotFound("")
    }
}

object GetPendingUpdate {

  val layer: URLayer[PendingUpdateRepository, GetPendingUpdate] = ZLayer.fromFunction(GetPendingUpdate(_))

}

