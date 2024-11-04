package is.valsk.esper.domain

import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model}

sealed trait EsperError extends Exception

sealed trait DeviceApiError extends EsperError

sealed trait FirmwareDownloadError extends Exception with EsperError

case class DeviceNotFound(deviceId: DeviceId) extends EsperError

case class EmailDeliveryError(message: String, cause: Throwable) extends Exception("Failed to send email", cause) with EsperError

case class ManufacturerNotSupported(manufacturer: Manufacturer) extends Exception(s"Manufacturer not supported: $manufacturer") with EsperError with FirmwareDownloadError

case class ManufacturerIsEmpty() extends Exception(s"Manufacturer is empty") with EsperError

case class FirmwareDownloadFailed(message: String, manufacturer: Manufacturer, model: Model, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with FirmwareDownloadError

case class FailedToParseFirmwareResponse(message: String, manufacturer: Manufacturer, model: Model, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with FirmwareDownloadError

case class FirmwareDownloadLinkResolutionFailed(message: String, manufacturer: Manufacturer, model: Model, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with FirmwareDownloadError

case class FirmwareNotFound(message: String, manufacturer: Manufacturer, model: Model, version: Option[Version], cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with FirmwareDownloadError

case class PersistenceException(message: String, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with EsperError

case class EntityNotFound(entityId: String) extends Exception("Not found") with EsperError

case class MalformedVersion(version: String) extends Exception(s"Malformed version: $version") with DeviceApiError

case class ApiCallFailed(message: String, device: Device, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with DeviceApiError

case class FailedToParseApiResponse(message: String, device: Device, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with DeviceApiError