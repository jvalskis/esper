package is.valsk.esper.domain.errors

import is.valsk.esper.domain.{DeviceApiError, EntityNotFound, FirmwareNotFound, ManufacturerNotSupported}
import sttp.model.StatusCode

final case class HttpError(
    statusCode: StatusCode,
    message: String,
    cause: Throwable,
) extends RuntimeException(message, cause)

object HttpError {
  def decode(tuple: (StatusCode, String)): HttpError = HttpError(tuple._1, tuple._2, new RuntimeException(tuple._2))
  def encode(error: Throwable): (StatusCode, String) = error match {
    case e: ManufacturerNotSupported => (StatusCode.PreconditionFailed, e.getMessage)
    case e: DeviceApiError => (StatusCode.BadGateway, error.getMessage)
    case e: EntityNotFound => (StatusCode.NotFound, error.getMessage)
    case e: FirmwareNotFound => (StatusCode.NotFound, error.getMessage)
    case e => (StatusCode.InternalServerError, error.getMessage)
  }
}