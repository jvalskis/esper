package is.valsk.esper.hass.messages.responses

import is.valsk.esper.hass.messages.Type.typeDecoder
import is.valsk.esper.hass.messages.{ HassMessage, HassResponseMessage}
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class AuthRequired(
    `type`: String,
    ha_version: String,
) extends HassResponseMessage

object AuthRequired {
  implicit val decoder: JsonDecoder[AuthRequired] = DeriveJsonDecoder.gen[AuthRequired]
}