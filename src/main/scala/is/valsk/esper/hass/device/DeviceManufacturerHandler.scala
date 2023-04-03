package is.valsk.esper.hass.device

import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.model.Device
import zio.IO

trait DeviceManufacturerHandler {

  def toDomain(hassDevice: HassResult): IO[String, Device]
}

object DeviceManufacturerHandler {

  type Manufacturer = String
}