package is.valsk.esper.hass.device

import is.valsk.esper.device.DeviceDescriptor
import is.valsk.esper.errors.FirmwareDownloadError
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.model.Device
import zio.IO

trait DeviceManufacturerHandler {

  def toDomain(hassDevice: HassResult): IO[String, Device]

  def downloadFirmware(deviceDescriptor: DeviceDescriptor): IO[FirmwareDownloadError, String]
}

object DeviceManufacturerHandler {

  type Manufacturer = String
}