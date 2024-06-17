package is.valsk.esper.repositories

import is.valsk.esper.device.DeviceHandler
import is.valsk.esper.domain.Types.Manufacturer

trait ManufacturerRepository extends Repository[Manufacturer, DeviceHandler]