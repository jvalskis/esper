package is.valsk.esper.api.devices.endpoints

import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.repositories.DeviceRepository
import zio.http.Response
import zio.http.model.HttpError
import zio.json.*
import zio.{IO, URLayer, ZLayer}

class GetDevice(
    deviceRepository: DeviceRepository
) {

  def apply(deviceId: DeviceId): IO[HttpError, Response] = for {
    value <- deviceRepository.get(deviceId)
      .logError(s"Failed to get device $deviceId")
      .mapError(_ => HttpError.InternalServerError())// TODO error handling
  } yield Response.json(value.toJson)
}

object GetDevice {

  val layer: URLayer[DeviceRepository, GetDevice] = ZLayer.fromFunction(GetDevice(_))

}
