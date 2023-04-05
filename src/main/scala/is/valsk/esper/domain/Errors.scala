package is.valsk.esper.domain

import is.valsk.esper.domain.DeviceModel
import is.valsk.esper.domain.Types.Manufacturer

sealed trait EsperError extends Exception

sealed trait FirmwareDownloadError extends EsperError

case class ManufacturerNotSupported(manufacturer: Manufacturer) extends EsperError

case class FirmwareDownloadFailed(deviceDescriptor: DeviceModel, cause: Option[Throwable] = None) extends FirmwareDownloadError

case class FailedToParseFirmwareResponse(message: String, deviceDescriptor: DeviceModel, cause: Option[Throwable] = None) extends FirmwareDownloadError

case class FirmwareDownloadLinkResolutionFailed(message: String, deviceDescriptor: DeviceModel, cause: Option[Throwable] = None) extends FirmwareDownloadError