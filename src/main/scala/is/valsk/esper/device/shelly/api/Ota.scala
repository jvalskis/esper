package is.valsk.esper.device.shelly.api

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

  implicit val decoder: JsonDecoder[Ota] = DeriveJsonDecoder.gen[Ota]

  enum OtaStatus:
    case idle, pending, updating, unknown
  object OtaStatus:
    implicit val decoder: JsonDecoder[OtaStatus] = JsonDecoder[String].map(OtaStatus.valueOf)
  end OtaStatus
}