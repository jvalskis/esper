package is.valsk.esper.hass.messages.responses

import is.valsk.esper.hass.messages.Type.typeDecoder
import is.valsk.esper.hass.messages.{HassMessage, HassResponseMessage}
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class AuthOK(
    `type`: String,
    ha_version: String,
) extends HassMessage with HassResponseMessage

object AuthOK {
  implicit val decoder: JsonDecoder[AuthOK] = DeriveJsonDecoder.gen[AuthOK]
}