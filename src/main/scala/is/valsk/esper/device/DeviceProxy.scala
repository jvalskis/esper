package is.valsk.esper.device

import is.valsk.esper.domain.{Device, DeviceApiError, Version}
import zio.IO

trait DeviceProxy[V <: Version[V]] {

  def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, V]
}
