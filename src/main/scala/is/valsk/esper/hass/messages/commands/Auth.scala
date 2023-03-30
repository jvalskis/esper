package is.valsk.esper.hass.messages.commands

import is.valsk.esper.hass.messages.{HassMessage, HassRequestMessage, Type}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class Auth(
    `type`: String,
    access_token: String
) extends HassRequestMessage

object Auth {
  implicit val encoder: JsonEncoder[Auth] = DeriveJsonEncoder.gen[Auth]

  def apply(accessToken: String): Auth = Auth(Type.Auth.typeName, accessToken)
}