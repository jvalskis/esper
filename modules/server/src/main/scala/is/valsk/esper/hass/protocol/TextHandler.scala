package is.valsk.esper.hass.protocol

import is.valsk.esper.hass.messages.{HassResponseMessage, MessageParser}
import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.{WebSocketChannelEvent, WebSocketFrame}

class TextHandler(
    handleHassMessages: PartialHassResponseMessageHandler,
    messageParser: MessageParser[HassResponseMessage],
) extends ChannelHandler {

  override def get: PartialChannelHandler = {
    case (channel, _, Read(WebSocketFrame.Text(json))) =>
      val result = for {
        _ <- ZIO.logDebug(json)
        parsedMessage <- messageParser.parseMessage(json)
        _ <- handleHassMessages(HassResponseMessageContext(channel, parsedMessage))
      } yield ()
      result
        .catchAll(e => ZIO.logError(s"Failed to parse message: ${e.getMessage}. Message: $json"))
  }
}

object TextHandler {
  private val rest: HassResponseMessageHandler = new HassResponseMessageHandler {
    override def get: PartialHassResponseMessageHandler = {
      case HassResponseMessageContext(_, message) => ZIO.logWarning(s"Message not handled: $message")
    }
  }

  val layer: URLayer[List[HassResponseMessageHandler] & MessageParser[HassResponseMessage], TextHandler] = ZLayer {
    for {
      handlers <- ZIO.service[List[HassResponseMessageHandler]]
      parser <- ZIO.service[MessageParser[HassResponseMessage]]
      combinedHandler = (handlers :+ rest)
        .foldLeft(HassResponseMessageHandler.empty) { (a, b) => a orElse b.get }
    } yield TextHandler(combinedHandler, parser)
  }
}