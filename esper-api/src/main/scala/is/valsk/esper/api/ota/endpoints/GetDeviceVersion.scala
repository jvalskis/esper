package is.valsk.esper.api.ota.endpoints

import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.domain.Version.encoder
import is.valsk.esper.domain.{ApiCallFailed, DeviceNotFound, FailedToParseApiResponse, MalformedVersion, ManufacturerNotSupported}
import is.valsk.esper.services.OtaService
import zio.http.Response
import zio.http.model.HttpError
import zio.json.*
import zio.{IO, URLayer, ZLayer}

class GetDeviceVersion(
    otaService: OtaService
) {

  def apply(deviceId: DeviceId): IO[HttpError, Response] = {
    for {
      version <- otaService.getCurrentFirmwareVersion(deviceId)
    } yield Response.json(version.toJson)
  }
    .mapError {
      case e: DeviceNotFound => HttpError.NotFound("")
      case e: MalformedVersion => HttpError.BadRequest(e.getMessage)
      case e: ApiCallFailed => HttpError.BadGateway(e.getMessage)
      case e: ManufacturerNotSupported => HttpError.PreconditionFailed(e.getMessage)
      case e: FailedToParseApiResponse => HttpError.BadGateway(e.getMessage)
      case e => HttpError.InternalServerError(e.getMessage)
    }
}

object GetDeviceVersion {

  val layer: URLayer[OtaService, GetDeviceVersion] = ZLayer.fromFunction(GetDeviceVersion(_))

}
