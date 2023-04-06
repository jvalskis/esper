package is.valsk.esper.api.devices

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.DeviceProxy
import is.valsk.esper.device.shelly.ShellyDeviceHandler.ShellyDevice
import is.valsk.esper.domain.SemanticVersion.encoder
import is.valsk.esper.repositories.DeviceRepository
import zio.http.Response
import zio.http.model.{HttpError, Status}
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class GetDeviceVersion(
    deviceProxy: DeviceProxy[ShellyDevice],
    deviceRepository: DeviceRepository
) {

  def apply(deviceId: NonEmptyString): IO[HttpError, Response] = for {
    device <- deviceRepository.get(deviceId)
      .mapError(_ => HttpError.InternalServerError())
    response <- device match {
      case Some(value) =>
        deviceProxy.getCurrentFirmwareVersion(value).map(version => Response.json(version.toJson))
          .mapError(e => HttpError.BadGateway(e.getMessage))
      case None =>
        ZIO.fail(HttpError.NotFound(""))
    }
  } yield response
}

object GetDeviceVersion {

  val layer: URLayer[DeviceProxy[ShellyDevice] & DeviceRepository, GetDeviceVersion] = ZLayer {
    for {
      deviceProxy <- ZIO.service[DeviceProxy[ShellyDevice]]
      deviceRepository <- ZIO.service[DeviceRepository]
    } yield GetDeviceVersion(deviceProxy, deviceRepository)
  }

}
