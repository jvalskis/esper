package is.valsk.esper.device

import is.valsk.esper.domain.{Device, DeviceApiError, Firmware, Version}
import zio.IO

trait DeviceProxy[V <: Version[V]] {

  def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, V]

  def flashFirmware(firmware: Firmware): IO[DeviceApiError, Unit]
}
