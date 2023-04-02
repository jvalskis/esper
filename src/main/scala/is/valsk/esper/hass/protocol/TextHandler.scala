package is.valsk.esper.hass.protocol

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.{AuthInvalid, AuthOK, AuthRequired, Result}
import is.valsk.esper.hass.messages.{HassResponseMessage, MessageParser}
import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import is.valsk.esper.services.DeviceRepository
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

class TextHandler(
    handleHassMessages: PartialHassResponseMessageHandler,
    messageParser: MessageParser[HassResponseMessage],
) extends ProtocolHandler {

  override def get: PartialChannelHandler = {
    case ChannelEvent(channel, ChannelRead(WebSocketFrame.Text(json))) =>
      val result = for {
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