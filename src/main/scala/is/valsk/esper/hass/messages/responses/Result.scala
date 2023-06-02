package is.valsk.esper.hass.messages.responses

import is.valsk.esper.hass.messages.{HassIdentifiableMessage, HassResponseMessage}
import is.valsk.esper.hass.messages.Type.typeDecoder
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class Result(
    `type`: String,
    id: Int,
    success: Boolean,
    result: Option[Seq[HassResult]],
    error: Option[HassError]
) extends HassResponseMessage with HassIdentifiableMessage

case class HassResult(
    id: String,
    area_id: Option[String],
    configuration_url: Option[String],
    hw_version: Option[String],
    model: Option[String],
    name: String,
    name_by_user: Option[String],
    sw_version: Option[String],
    manufacturer: Option[String]
)

case class HassError(
    code: String,
    message: String
)

object Result {
  implicit val hassErrorDecoder: JsonDecoder[HassError] = DeriveJsonDecoder.gen[HassError]
  implicit val hassResultDecoder: JsonDecoder[HassResult] = DeriveJsonDecoder.gen[HassResult]
  implicit val decoder: JsonDecoder[Result] = DeriveJsonDecoder.gen[Result]
}