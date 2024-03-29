package is.valsk.esper.hass.messages.responses

import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class TypedMessage(
    `type`: String
)

object TypedMessage {
  implicit val decoder: JsonDecoder[TypedMessage] = DeriveJsonDecoder.gen[TypedMessage]
}