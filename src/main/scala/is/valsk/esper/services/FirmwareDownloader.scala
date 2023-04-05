package is.valsk.esper.services

import is.valsk.esper.domain.{DeviceModel, EsperError}
import zio.IO

trait FirmwareDownloader {

  def downloadFirmware(deviceDescriptor: DeviceModel): IO[EsperError, Unit]

}