package is.valsk.esper.api.ota.endpoints

import is.valsk.esper.device.DeviceStatus.encoder
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.domain.{ApiCallFailed, EntityNotFound, FailedToParseApiResponse, MalformedVersion, ManufacturerNotSupported}
import is.valsk.esper.services.OtaService
import zio.http.Response
import zio.http.model.HttpError
import zio.json.*
import zio.{IO, URLayer, ZLayer}

class GetDeviceStatus(
    otaService: OtaService
) {

  def apply(deviceId: DeviceId): IO[HttpError, Response] = {
    for {
      status <- otaService.getDeviceStatus(deviceId)
    } yield Response.json(status.toJson)
  }
    .mapError {
      case e: EntityNotFound => HttpError.NotFound("")
      case e: MalformedVersion => HttpError.BadRequest(e.getMessage)
      case e: ApiCallFailed => HttpError.BadGateway(e.getMessage)
      case e: ManufacturerNotSupported => HttpError.PreconditionFailed(e.getMessage)
      case e: FailedToParseApiResponse => HttpError.BadGateway(e.getMessage)
      case e => HttpError.InternalServerError(e.getMessage)
    }
}

object GetDeviceStatus {

  val layer: URLayer[OtaService, GetDeviceStatus] = ZLayer.fromFunction(GetDeviceStatus(_))

}
