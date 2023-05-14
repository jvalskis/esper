package is.valsk.esper.api.devices

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.repositories.DeviceRepository
import zio.http.Response
import zio.http.model.{HttpError, Status}
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class GetDevice(
    deviceRepository: DeviceRepository
) {

  def apply(deviceId: NonEmptyString): IO[HttpError, Response] = for {
    device <- deviceRepository.get(deviceId)
      .mapError(_ => HttpError.InternalServerError())
    response <- device match {
      case Some(value) => ZIO.succeed(Response.json(value.toJson))
      case None => ZIO.fail(HttpError.NotFound(""))
    }
  } yield response
}

object GetDevice {

  val layer: URLayer[DeviceRepository, GetDevice] = ZLayer.fromFunction(GetDevice(_))

}
