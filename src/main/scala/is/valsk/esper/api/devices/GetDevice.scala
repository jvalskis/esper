package is.valsk.esper.api.devices

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.repositories.DeviceRepository
import zio.http.Response
import zio.http.model.Status
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class GetDevice(
    deviceRepository: DeviceRepository
) {

  def apply(deviceId: NonEmptyString): IO[Response, Response] = for {
    device <- deviceRepository.get(deviceId)
      .mapError(_ => Response.status(Status.InternalServerError))
    response = device match {
      case Some(value) => Response.json(value.toJson)
      case None => Response.status(Status.NotFound)
    }
  } yield response
}

object GetDevice {

  val layer: URLayer[DeviceRepository, GetDevice] = ZLayer {
    for {
      deviceRepository <- ZIO.service[DeviceRepository]
    } yield GetDevice(deviceRepository)
  }

}
