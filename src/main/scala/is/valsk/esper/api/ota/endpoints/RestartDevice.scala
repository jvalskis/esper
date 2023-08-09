package is.valsk.esper.api.ota.endpoints

import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.domain.{ApiCallFailed, FailedToParseApiResponse, MalformedVersion, ManufacturerNotSupported}
import is.valsk.esper.services.OtaService
import zio.http.Response
import zio.http.model.HttpError
import zio.{IO, URLayer, ZLayer}

class RestartDevice(
    otaService: OtaService
) {

  def apply(deviceId: DeviceId): IO[HttpError, Response] = {
    for {
      _ <- otaService.restartDevice(deviceId)
    } yield Response.ok
  }
    .mapError {
      case e: MalformedVersion => HttpError.BadRequest(e.getMessage)
      case e: ApiCallFailed => HttpError.BadGateway(e.getMessage)
      case e: ManufacturerNotSupported => HttpError.PreconditionFailed(e.getMessage)
      case e: FailedToParseApiResponse => HttpError.BadGateway(e.getMessage)
      case e => HttpError.InternalServerError(e.getMessage)
    }
}

object RestartDevice {

  val layer: URLayer[OtaService, RestartDevice] = ZLayer.fromFunction(RestartDevice(_))

}
