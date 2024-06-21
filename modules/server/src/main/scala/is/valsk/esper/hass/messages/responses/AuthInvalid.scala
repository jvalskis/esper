package is.valsk.esper.hass.messages.responses

import is.valsk.esper.hass.messages.HassResponseMessage
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class AuthInvalid(
    `type`: String,
    message: String,
) extends HassResponseMessage

object AuthInvalid {
  given decoder: JsonDecoder[AuthInvalid] = DeriveJsonDecoder.gen[AuthInvalid]
}