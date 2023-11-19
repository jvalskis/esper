package is.valsk.esper.model.db

import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.domain.Version

case class PendingUpdateDto(
    id: DeviceId,
    version: Version
)
