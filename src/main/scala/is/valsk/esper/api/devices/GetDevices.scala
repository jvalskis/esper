package is.valsk.esper.api.devices

import is.valsk.esper.api.FirmwareApi
import is.valsk.esper.repositories.DeviceRepository
import zio.http.model.{HttpError, Status}
import zio.http.{Request, Response}
import zio.json.*
import zio.{IO, Task, UIO, URLayer, ZIO, ZLayer}

class GetDevices(
    deviceRepository: DeviceRepository
) {

  def apply(): IO[HttpError, Response] = for {
    deviceList <- deviceRepository.getAll
      .mapError(_ => HttpError.InternalServerError())
    response <- ZIO.succeed(Response.json(deviceList.toJson))
  } yield response
}

object GetDevices {

  val layer: URLayer[DeviceRepository, GetDevices] = ZLayer {
    for {
      deviceRepository <- ZIO.service[DeviceRepository]
    } yield GetDevices(deviceRepository)
  }

}

