package is.valsk.esper.device

import is.valsk.esper.device.DeviceStatus.UpdateStatus
import is.valsk.esper.domain.Version
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class FlashResult(
    previousVersion: Version,
    currentVersion: Version,
    updateStatus: UpdateStatus
)

case object FlashResult {

  import is.valsk.esper.device.DeviceStatus.UpdateStatus.{decoder, encoder}
  import is.valsk.esper.domain.Version.{decoder, encoder}

  implicit val decoder: JsonDecoder[FlashResult] = DeriveJsonDecoder.gen[FlashResult]
  implicit val encoder: JsonEncoder[FlashResult] = DeriveJsonEncoder.gen[FlashResult]

}
