package is.valsk.esper.services

import is.valsk.esper.errors.EsperError
import is.valsk.esper.model.DeviceModel
import zio.IO

trait FirmwareDownloader {

  def downloadFirmware(deviceDescriptor: DeviceModel): IO[EsperError, Unit]

}