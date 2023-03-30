package is.valsk.esper.hass.protocol

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.messages.HassMessageParser.parseMessage
import is.valsk.esper.hass.messages.HassResponseMessage
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.{AuthInvalid, AuthOK, AuthRequired, Result}
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

class HassSocketMessageHandler(
    onConnect: Function[Channel[WebSocketFrame], RIO[EsperConfig, Unit]],
    pf: PartialFunction[WebSocketChannelEvent, RIO[EsperConfig, Unit]] = PartialFunction.empty
) {

  def apply(hassResponseMessageHandler: PartialFunction[HassResponseMessage, RIO[EsperConfig, Unit]]): PartialFunction[WebSocketChannelEvent, RIO[EsperConfig, Unit]] = {
    case ChannelEvent(channel, UserEventTriggered(event)) =>
      event match {
        case HandshakeComplete => handshakeCompleteHandler(channel)
        case HandshakeTimeout => handshakeTimeoutHandler(channel)
      }

    case ChannelEvent(channel, ChannelRegistered) =>
      channelRegisteredHandler(channel)

    case ChannelEvent(channel, ChannelUnregistered) =>
      channelUnregisteredHandler(channel)

    case ChannelEvent(channel, ChannelRead(WebSocketFrame.Ping)) =>
      pingHandler(channel)

    case ChannelEvent(channel, ChannelRead(WebSocketFrame.Text(json))) =>
      websocketTextFrameHandler(onConnect, hassResponseMessageHandler, channel, json)

    case message => (pf andThen (x => ZIO.logInfo(s"Unhandled: $x"))).apply(message)
  }

  protected def pingHandler(ch: Channel[WebSocketFrame]): RIO[EsperConfig, Unit] = {
    for {
      _ <- ZIO.logInfo("Received PING - sending PONG")
      _ <- ch.writeAndFlush(WebSocketFrame.Pong)
    } yield ()
  }

  protected def channelRegisteredHandler(ch: Channel[WebSocketFrame]): RIO[EsperConfig, Unit] = {
    ZIO.logInfo("Connection opened!")
  }

  protected def channelUnregisteredHandler(ch: Channel[WebSocketFrame]): RIO[EsperConfig, Unit] = {
    ZIO.logInfo("Connection closed!")
  }

  protected def handshakeCompleteHandler(ch: Channel[WebSocketFrame]): RIO[EsperConfig, Unit] = {
    ZIO.logInfo("Connection started!")
  }

  protected def handshakeTimeoutHandler(ch: Channel[WebSocketFrame]): RIO[EsperConfig, Unit] = {
    ZIO.logInfo("Connection failed!")
  }

  protected def websocketTextFrameHandler(onConnect: Function[Channel[WebSocketFrame], RIO[EsperConfig, Unit]], pf: PartialFunction[HassResponseMessage, RIO[EsperConfig, Unit]], channel: Channel[WebSocketFrame], json: String): RIO[EsperConfig, Unit] = {
    val rest: PartialFunction[HassResponseMessage, RIO[EsperConfig, Unit]] = {
      case x => ZIO.logWarning(s"Message not handled: $x")
    }

    val result = for {
      parsedMessage <- parseMessage(json)
      _ <- (AuthentificationProtocol(channel, onConnect) orElse pf orElse rest).apply(parsedMessage)
    } yield ()
    result
      .catchAll(e => ZIO.logError(s"Failed to parse message: ${e.getMessage}. Message: $json"))
  }
}
