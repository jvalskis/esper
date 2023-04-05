package is.valsk.esper.repositories

import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.repositories.Repository
import is.valsk.esper.domain.Types.Manufacturer
import zio.UIO

trait ManufacturerRepository extends Repository[Manufacturer, DeviceManufacturerHandler]