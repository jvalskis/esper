package is.valsk.esper.errors

import is.valsk.esper.device.DeviceDescriptor
import is.valsk.esper.types.Manufacturer

sealed trait EsperError extends Exception

sealed trait FirmwareDownloadError extends EsperError

case class ManufacturerNotSupported(manufacturer: Manufacturer) extends EsperError

case class FirmwareDownloadFailed(deviceDescriptor: DeviceDescriptor, cause: Option[Throwable] = None) extends FirmwareDownloadError

case class FailedToParseFirmwareResponse(message: String, deviceDescriptor: DeviceDescriptor, cause: Option[Throwable] = None) extends FirmwareDownloadError

case class FirmwareDownloadLinkResolutionFailed(message: String, deviceDescriptor: DeviceDescriptor, cause: Option[Throwable] = None) extends FirmwareDownloadError