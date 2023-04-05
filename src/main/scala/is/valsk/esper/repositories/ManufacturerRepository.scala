package is.valsk.esper.repositories

import is.valsk.esper.device.DeviceManufacturerHandler
import is.valsk.esper.repositories.Repository
import is.valsk.esper.domain.Types.Manufacturer
import is.valsk.esper.hass.HassToDomainMapper
import zio.UIO

trait ManufacturerRepository extends Repository[Manufacturer, DeviceManufacturerHandler with HassToDomainMapper]