package is.valsk.esper.hass.messages.responses

import is.valsk.esper.hass.messages.HassResponseMessage
import is.valsk.esper.hass.messages.Type.typeDecoder
import zio.json.{DeriveJsonDecoder, JsonDecoder}

case class Result(
    `type`: String,
    id: Int,
) extends HassResponseMessage

object Result {
  implicit val decoder: JsonDecoder[Result] = DeriveJsonDecoder.gen[Result]
}