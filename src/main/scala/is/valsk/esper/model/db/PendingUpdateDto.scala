package is.valsk.esper.model.db

import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model}
import is.valsk.esper.domain.Version

case class PendingUpdateDto(
    id: DeviceId,
    version: Version
)
