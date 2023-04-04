package is.valsk.esper.hass.device

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.DeviceDescriptor
import is.valsk.esper.errors.FirmwareDownloadFailed
import is.valsk.esper.hass.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.model.Device
import is.valsk.esper.utils.SemanticVersion
import zio.IO

trait DeviceManufacturerHandler {

  def toDomain(hassDevice: HassResult): IO[String, Device]

  def downloadFirmware(deviceDescriptor: DeviceDescriptor): IO[FirmwareDownloadFailed, FirmwareDescriptor]
}

object DeviceManufacturerHandler {

  case class FirmwareDescriptor(path: String, version: String)
}