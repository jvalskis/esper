package is.valsk.esper.hass.messages.responses

import is.valsk.esper.hass.messages.HassResponseMessage
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class AuthRequired(
    `type`: String,
    ha_version: String,
) extends HassResponseMessage

object AuthRequired {
  given decoder: JsonDecoder[AuthRequired] = DeriveJsonDecoder.gen[AuthRequired]
}