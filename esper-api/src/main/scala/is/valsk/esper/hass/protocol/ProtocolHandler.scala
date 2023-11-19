package is.valsk.esper.hass.protocol

import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}

class ProtocolHandler extends ChannelHandler {

  override def get: PartialChannelHandler = {
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
  }

  protected def pingHandler(ch: Channel[WebSocketFrame]): Task[Unit] = {
    for {
      _ <- ZIO.logInfo("Received PING - sending PONG")
      _ <- ch.writeAndFlush(WebSocketFrame.Pong)
    } yield ()
  }

  protected def channelRegisteredHandler(ch: Channel[WebSocketFrame]): Task[Unit] =
    ZIO.logInfo("Connection opened!")

  protected def channelUnregisteredHandler(ch: Channel[WebSocketFrame]): Task[Unit] =
    ZIO.logInfo("Connection closed!")

  protected def handshakeCompleteHandler(ch: Channel[WebSocketFrame]): Task[Unit] =
    ZIO.logInfo("Connection started!")

  protected def handshakeTimeoutHandler(ch: Channel[WebSocketFrame]): Task[Unit] =
    ZIO.logInfo("Connection failed!")
}

object ProtocolHandler {
  val layer: ULayer[ProtocolHandler] = ZLayer.succeed(ProtocolHandler())
}
