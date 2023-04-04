package is.valsk.esper.services

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.model.Device

trait DeviceRepository extends Repository[NonEmptyString, Device]
