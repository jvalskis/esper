package is.valsk.esper.domain

import is.valsk.esper.domain.DeviceModel
import is.valsk.esper.domain.Types.Manufacturer

sealed trait EsperError extends Exception

sealed trait DeviceApiError extends EsperError

sealed trait PersistenceException extends EsperError

sealed trait FirmwareDownloadError extends Exception with EsperError

case class ManufacturerNotSupported(manufacturer: Manufacturer) extends Exception(s"Manufacturer not supported: $manufacturer") with EsperError with FirmwareDownloadError

case class FirmwareDownloadFailed(message: String, deviceModel: DeviceModel, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with FirmwareDownloadError

case class FailedToParseFirmwareResponse(message: String, deviceModel: DeviceModel, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with FirmwareDownloadError

case class FirmwareDownloadLinkResolutionFailed(message: String, deviceModel: DeviceModel, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with FirmwareDownloadError

case class FailedToStoreFirmware(message: String, deviceModel: DeviceModel, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with PersistenceException

case class MalformedVersion(version: String, device: Device) extends Exception(version) with DeviceApiError

case class ApiCallFailed(message: String, device: Device, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with DeviceApiError

case class FailedToParseApiResponse(message: String, device: Device, cause: Option[Throwable] = None) extends Exception(message, cause.orNull) with DeviceApiError