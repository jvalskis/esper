package is.valsk.esper.device

import is.valsk.esper.device.DeviceStatus.UpdateStatus
import is.valsk.esper.device.shelly.api.Ota
import is.valsk.esper.device.shelly.api.Ota.OtaStatus
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class DeviceStatus(
    updateStatus: UpdateStatus
)

case object DeviceStatus {

  import is.valsk.esper.device.DeviceStatus.UpdateStatus.{decoder, encoder}

  implicit val decoder: JsonDecoder[DeviceStatus] = DeriveJsonDecoder.gen[DeviceStatus]
  implicit val encoder: JsonEncoder[DeviceStatus] = DeriveJsonEncoder.gen[DeviceStatus]

  enum UpdateStatus:
    case idle, pending, updating, unknown

  object UpdateStatus:

    implicit val decoder: JsonDecoder[UpdateStatus] = DeriveJsonDecoder.gen[UpdateStatus]
    implicit val encoder: JsonEncoder[UpdateStatus] = DeriveJsonEncoder.gen[UpdateStatus]
  end UpdateStatus
}
