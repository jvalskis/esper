package is.valsk.esper.api.ota.endpoints

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.{DeviceProxy, DeviceProxyRegistry}
import is.valsk.esper.domain.{ApiCallFailed, FailedToParseApiResponse, MalformedVersion, ManufacturerNotSupported}
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.domain.Version.encoder
import is.valsk.esper.repositories.DeviceRepository
import is.valsk.esper.services.OtaService
import zio.http.Response
import zio.http.model.{HttpError, Status}
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class GetDeviceVersion(
    otaService: OtaService
) {

  def apply(deviceId: DeviceId): IO[HttpError, Response] = {
    for {
      version <- otaService.getCurrentFirmwareVersion(deviceId)
    } yield Response.json(version.toJson)
  }
    .mapError {
      case e@MalformedVersion(version, device) => HttpError.BadRequest(e.getMessage)
      case e@ApiCallFailed(message, device, cause) => HttpError.BadGateway(e.getMessage)
      case e@ManufacturerNotSupported(manufacturer) => HttpError.PreconditionFailed(e.getMessage)
      case e@FailedToParseApiResponse(message, device, cause) => HttpError.BadGateway(e.getMessage)
    }
}

object GetDeviceVersion {

  val layer: URLayer[OtaService, GetDeviceVersion] = ZLayer.fromFunction(GetDeviceVersion(_))

}
