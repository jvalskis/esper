package is.valsk.esper.hass.messages

import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.*
import is.valsk.esper.hass.messages.responses.TypedMessage.decoder
import zio.*
import zio.json.*

class HassResponseMessageParser extends MessageParser[HassResponseMessage] {

  def parseMessage(json: String): IO[ParseError, HassResponseMessage] = {
    json.fromJson[TypedMessage] match {
      case Left(value) =>
        ZIO.fail(ParseError(value))
      case Right(Type(messageType)) =>
        val parseResult = messageType match {
          case Type.AuthRequired => json.fromJson[AuthRequired]
          case Type.AuthOK => json.fromJson[AuthOK]
          case Type.AuthInvalid => json.fromJson[AuthInvalid]
          case Type.Result => json.fromJson[Result]
          case other => Left(s"Unsupported type: $other")
        }
        parseResult match
          case Left(value) => ZIO.fail(ParseError(value))
          case Right(value) => ZIO.succeed(value)
      case _ => ZIO.fail(ParseError("Failed to extract HASS message type"))
    }
  }
}

object HassResponseMessageParser {

  val layer: ULayer[MessageParser[HassResponseMessage]] = ZLayer.succeed(HassResponseMessageParser())
}
