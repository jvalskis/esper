package is.valsk.esper.device

import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.types.Manufacturer
import zio.UIO

trait ManufacturerRegistry {

  def findHandler(manufacturer: Manufacturer): UIO[Option[DeviceManufacturerHandler]]
}
