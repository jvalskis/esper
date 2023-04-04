package is.valsk.esper.device

import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.services.Repository
import is.valsk.esper.types.Manufacturer
import zio.UIO

trait ManufacturerRepository extends Repository[Manufacturer, DeviceManufacturerHandler]