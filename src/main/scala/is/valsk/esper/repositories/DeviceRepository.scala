package is.valsk.esper.repositories

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.model.Device
import is.valsk.esper.repositories.Repository

trait DeviceRepository extends Repository[NonEmptyString, Device]
