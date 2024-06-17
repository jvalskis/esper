package is.valsk.esper.api.devices.endpoints

import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.repositories.DeviceRepository
import zio.http.Response
import zio.http.model.HttpError
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class GetDevice(
    deviceRepository: DeviceRepository
) {

  def apply(deviceId: DeviceId): IO[HttpError, Response] = for {
    maybeDevice <- deviceRepository.getOpt(deviceId)
      .mapError(_ => HttpError.InternalServerError())
    result <- ZIO.fromOption(maybeDevice)
      .mapError(_ => HttpError.NotFound(deviceId.toString))
      .map(device => Response.json(device.toJson))
  } yield result
}

object GetDevice {

  val layer: URLayer[DeviceRepository, GetDevice] = ZLayer.fromFunction(GetDevice(_))

}
