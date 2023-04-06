package is.valsk.esper.api.devices

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.DeviceProxy
import is.valsk.esper.device.shelly.ShellyDeviceHandler.ShellyDevice
import is.valsk.esper.domain.SemanticVersion.encoder
import is.valsk.esper.repositories.DeviceRepository
import zio.http.Response
import zio.http.model.Status
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class GetDeviceVersion(
    deviceProxy: DeviceProxy[ShellyDevice],
    deviceRepository: DeviceRepository
) {

  def apply(deviceId: NonEmptyString): IO[Response, Response] = (for {
    device <- deviceRepository.get(deviceId)
    response <- device match {
      case Some(value) =>
        for {
          version <- deviceProxy.getCurrentFirmwareVersion(value)
        } yield Response.json(version.toJson)
      case None =>
        ZIO.succeed(Response.status(Status.NotFound))
    }
  } yield response)
    .catchSome { case e: Throwable => ZIO.logError(e.getMessage) *> ZIO.fail(e) }
    .mapError(_ => Response.status(Status.BadRequest))
}

object GetDeviceVersion {

  val layer: URLayer[DeviceProxy[ShellyDevice] & DeviceRepository, GetDeviceVersion] = ZLayer {
    for {
      deviceProxy <- ZIO.service[DeviceProxy[ShellyDevice]]
      deviceRepository <- ZIO.service[DeviceRepository]
    } yield GetDeviceVersion(deviceProxy, deviceRepository)
  }

}
