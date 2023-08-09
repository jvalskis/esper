package is.valsk.esper.api.devices.endpoints

import is.valsk.esper.repositories.DeviceRepository
import zio.http.Response
import zio.http.model.HttpError
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class ListDevices(
    deviceRepository: DeviceRepository
) {

  def apply(): IO[HttpError, Response] = for {
    deviceList <- deviceRepository.getAll
      .logError("Failed to get devices")
      .mapError(_ => HttpError.InternalServerError())
    response <- ZIO.succeed(Response.json(deviceList.toJson))
  } yield response
}

object ListDevices {

  val layer: URLayer[DeviceRepository, ListDevices] = ZLayer.fromFunction(ListDevices(_))

}

