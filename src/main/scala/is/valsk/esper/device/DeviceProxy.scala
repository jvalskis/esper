package is.valsk.esper.device

import is.valsk.esper.domain.Types.Manufacturer
import is.valsk.esper.domain.*
import zio.IO

trait DeviceProxy {

  def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, Version]

  def flashFirmware(device: Device, firmware: Firmware): IO[DeviceApiError, Unit]
}