package is.valsk.esper.device.shelly.api

import is.valsk.esper.domain.DeviceStatus
import is.valsk.esper.domain.DeviceStatus.UpdateStatus
import is.valsk.esper.device.shelly.api.Ota.OtaStatus
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class Ota(
    status: OtaStatus,
    has_update: Boolean,
    new_version: String,
    old_version: String,
    beta_version: Option[String]
)

object Ota {

  import is.valsk.esper.device.shelly.api.Ota.OtaStatus.decoder

  given decoder: JsonDecoder[Ota] = DeriveJsonDecoder.gen[Ota]

  enum OtaStatus:
    case idle, pending, updating, unknown

  object OtaStatus:
    given decoder: JsonDecoder[OtaStatus] = JsonDecoder[String].map(OtaStatus.valueOf)
  end OtaStatus

  extension (otaStatus: OtaStatus)
    def mapToUpdateStatus: UpdateStatus = otaStatus match {
      case OtaStatus.idle => UpdateStatus.idle
      case OtaStatus.pending => UpdateStatus.pending
      case OtaStatus.updating => UpdateStatus.updating
      case OtaStatus.unknown => UpdateStatus.unknown
    }
}