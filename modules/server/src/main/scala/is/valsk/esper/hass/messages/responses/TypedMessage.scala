package is.valsk.esper.hass.messages.responses

import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class TypedMessage(
    `type`: String
)

object TypedMessage {
  given decoder: JsonDecoder[TypedMessage] = DeriveJsonDecoder.gen[TypedMessage]
}