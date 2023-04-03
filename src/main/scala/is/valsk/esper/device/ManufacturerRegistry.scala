package is.valsk.esper.device

import is.valsk.esper.hass.device.DeviceManufacturerHandler
import zio.UIO

trait ManufacturerRegistry {

  def findHandler(manufacturer: String): UIO[Option[DeviceManufacturerHandler]]
}
