package is.valsk.esper.domain

import is.valsk.esper.domain.DeviceStatus.UpdateStatus
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class DeviceStatus(
    updateStatus: UpdateStatus
)

case object DeviceStatus {

  import is.valsk.esper.domain.DeviceStatus.UpdateStatus.{decoder, encoder}

  given decoder: JsonDecoder[DeviceStatus] = DeriveJsonDecoder.gen[DeviceStatus]
  given encoder: JsonEncoder[DeviceStatus] = DeriveJsonEncoder.gen[DeviceStatus]

  enum UpdateStatus:
    case idle, pending, updating, unknown, done

  object UpdateStatus:

    given decoder: JsonDecoder[UpdateStatus] = DeriveJsonDecoder.gen[UpdateStatus]
    given encoder: JsonEncoder[UpdateStatus] = DeriveJsonEncoder.gen[UpdateStatus]
  end UpdateStatus
}
