package is.valsk.esper.api.devices.endpoints

import is.valsk.esper.api.firmware.FirmwareApi
import is.valsk.esper.repositories.{DeviceRepository, PendingUpdateRepository}
import zio.http.model.{HttpError, Status}
import zio.http.{Request, Response}
import zio.json.*
import zio.{IO, Task, UIO, URLayer, ZIO, ZLayer}

class GetPendingUpdates(
    pendingUpdateRepository: PendingUpdateRepository
) {

  def apply(): IO[HttpError, Response] = for {
    deviceList <- pendingUpdateRepository.getAll
      .logError("Failed to get devices")
      .mapError(_ => HttpError.InternalServerError())
    response <- ZIO.succeed(Response.json(deviceList.toJson))
  } yield response
}

object GetPendingUpdates {

  val layer: URLayer[PendingUpdateRepository, GetPendingUpdates] = ZLayer.fromFunction(GetPendingUpdates(_))

}

