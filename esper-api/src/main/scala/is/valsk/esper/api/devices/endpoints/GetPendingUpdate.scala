package is.valsk.esper.api.devices.endpoints

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
      maybePendingUpdate <- pendingUpdateRepository.getOpt(deviceId)
        .mapError(_ => HttpError.InternalServerError())
      response <- ZIO.fromOption(maybePendingUpdate)
        .map(device => Response.json(device.toJson))
        .mapError(_ => HttpError.NotFound(deviceId.toString))
    } yield response
  }
}

object GetPendingUpdate {

  val layer: URLayer[PendingUpdateRepository, GetPendingUpdate] = ZLayer.fromFunction(GetPendingUpdate(_))

}

