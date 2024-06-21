package is.valsk.esper.device

import is.valsk.esper.domain.Types.Manufacturer
import is.valsk.esper.hass.HassToDomainMapper

trait DeviceHandler extends DeviceManufacturerHandler with HassToDomainMapper with DeviceProxy {
  def supportedManufacturer: Manufacturer
}