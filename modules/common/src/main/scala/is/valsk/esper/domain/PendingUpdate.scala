package is.valsk.esper.domain

import is.valsk.esper.domain.{Device, Version}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class PendingUpdate(
    device: Device,
    version: Version
)

object PendingUpdate {

  import is.valsk.esper.domain.Device.{decoder, encoder}

  given encoder: JsonEncoder[PendingUpdate] = DeriveJsonEncoder.gen[PendingUpdate]
  given decoder: JsonDecoder[PendingUpdate] = DeriveJsonDecoder.gen[PendingUpdate]
}