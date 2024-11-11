package is.valsk.esper.model.db

import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.domain.Version

import java.time.LocalDateTime

case class PendingUpdateDto(
    id: DeviceId,
    version: Version,
    lastNotification: Option[LocalDateTime],
)
