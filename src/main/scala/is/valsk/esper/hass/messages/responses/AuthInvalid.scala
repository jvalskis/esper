package is.valsk.esper.hass.messages.responses

import is.valsk.esper.hass.messages.Type.typeDecoder
import is.valsk.esper.hass.messages.{HassMessage, HassResponseMessage}
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class AuthInvalid(
    `type`: String,
    message: String,
) extends HassResponseMessage

object AuthInvalid {
  implicit val decoder: JsonDecoder[AuthInvalid] = DeriveJsonDecoder.gen[AuthInvalid]
}