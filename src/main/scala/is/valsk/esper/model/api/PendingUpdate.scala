package is.valsk.esper.model.api

import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model}
import is.valsk.esper.domain.{Device, Version}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class PendingUpdate(
    device: Device,
    version: Version
)

object PendingUpdate {

  import is.valsk.esper.domain.Device.{decoder, encoder}
  import is.valsk.esper.domain.Types.NonEmptyStringImplicits.{decoder, encoder}

  implicit val encoder: JsonEncoder[PendingUpdate] = DeriveJsonEncoder.gen[PendingUpdate]
  implicit val decoder: JsonDecoder[PendingUpdate] = DeriveJsonDecoder.gen[PendingUpdate]
}