package is.valsk.esper.api.devices

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.{DeviceProxy, DeviceProxyRegistry}
import is.valsk.esper.domain.Version.encoder
import is.valsk.esper.repositories.DeviceRepository
import zio.http.Response
import zio.http.model.{HttpError, Status}
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class GetDeviceVersion(
    deviceProxyRegistry: DeviceProxyRegistry,
    deviceRepository: DeviceRepository
) {

  def apply(deviceId: NonEmptyString): IO[HttpError, Response] = for {
    device <- deviceRepository.get(deviceId)
      .mapError(_ => HttpError.InternalServerError())
      .flatMap {
        case Some(value) => ZIO.succeed(value)
        case None => ZIO.fail(HttpError.NotFound(""))
      }
    deviceProxy <- deviceProxyRegistry.selectProxy(device.manufacturer)
      .mapError(e => HttpError.PreconditionFailed(e.getMessage))
    response <- deviceProxy.getCurrentFirmwareVersion(device)
      .map(version => Response.json(version.toJson))
      .mapError(e => HttpError.BadGateway(e.getMessage))
  } yield response
}

object GetDeviceVersion {

  val layer: URLayer[DeviceProxyRegistry & DeviceRepository, GetDeviceVersion] = ZLayer.fromFunction(GetDeviceVersion(_, _))

}
