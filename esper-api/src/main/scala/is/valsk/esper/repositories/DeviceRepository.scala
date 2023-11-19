package is.valsk.esper.repositories

import is.valsk.esper.domain.Device
import is.valsk.esper.domain.Types.DeviceId

trait DeviceRepository extends Repository[DeviceId, Device]
