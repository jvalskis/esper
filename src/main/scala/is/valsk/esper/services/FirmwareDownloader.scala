package is.valsk.esper.services

import is.valsk.esper.device.DeviceDescriptor
import is.valsk.esper.errors.EsperError
import zio.IO

trait FirmwareDownloader {

  def downloadFirmware(deviceDescriptor: DeviceDescriptor): IO[EsperError, Unit]

}