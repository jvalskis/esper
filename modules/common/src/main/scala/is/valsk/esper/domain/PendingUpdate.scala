package is.valsk.esper.domain

import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

import java.time.LocalDateTime

case class PendingUpdate(
    device: Device,
    version: Version,
    lastNotification: Option[LocalDateTime] = None,
)

object PendingUpdate {

  import is.valsk.esper.domain.Device.{decoder, encoder}

  given encoder: JsonEncoder[PendingUpdate] = DeriveJsonEncoder.gen[PendingUpdate]
  given decoder: JsonDecoder[PendingUpdate] = DeriveJsonDecoder.gen[PendingUpdate]
}