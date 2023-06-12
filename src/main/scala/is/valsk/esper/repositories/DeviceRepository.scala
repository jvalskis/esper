package is.valsk.esper.repositories

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.domain.Device
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.repositories.Repository

trait DeviceRepository extends Repository[DeviceId, Device]
