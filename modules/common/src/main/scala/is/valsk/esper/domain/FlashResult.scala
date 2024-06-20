package is.valsk.esper.domain

import is.valsk.esper.domain.DeviceStatus.UpdateStatus
import is.valsk.esper.domain.Version
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class FlashResult(
    previousVersion: Version,
    currentVersion: Version,
    updateStatus: UpdateStatus
)

case object FlashResult {

  import is.valsk.esper.domain.DeviceStatus.UpdateStatus.{decoder, encoder}
  import is.valsk.esper.domain.Version.{decoder, encoder}

  given decoder: JsonDecoder[FlashResult] = DeriveJsonDecoder.gen[FlashResult]
  given encoder: JsonEncoder[FlashResult] = DeriveJsonEncoder.gen[FlashResult]

}
