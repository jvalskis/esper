package is.valsk.esper.errors

import is.valsk.esper.device.DeviceDescriptor
import is.valsk.esper.types.Manufacturer

sealed trait EsperError extends Exception

case class ManufacturerNotSupported(manufacturer: Manufacturer) extends EsperError

case class FirmwareDownloadFailed(deviceDescriptor: DeviceDescriptor, cause: Throwable) extends EsperError