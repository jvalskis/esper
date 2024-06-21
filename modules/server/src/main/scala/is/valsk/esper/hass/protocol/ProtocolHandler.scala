package is.valsk.esper.hass.protocol

import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.{WebSocketChannelEvent, WebSocketFrame}

class ProtocolHandler extends ChannelHandler {

  override def get: PartialChannelHandler = {
    case (channel, UserEventTriggered(event)) =>
      event match {
        case HandshakeComplete => handshakeCompleteHandler(channel)
        case HandshakeTimeout => handshakeTimeoutHandler(channel)
      }

    case (channel, Registered) =>
      channelRegisteredHandler(channel)

    case (channel, Unregistered) =>
      channelUnregisteredHandler(channel)

    case (channel, Read(WebSocketFrame.Ping)) =>
      pingHandler(channel)
  }

  protected def pingHandler(channel: WebSocketChannel): Task[Unit] = {
    for {
      _ <- ZIO.logInfo("Received PING - sending PONG")
      _ <- channel.send(Read(WebSocketFrame.Pong))
    } yield ()
  }

  protected def channelRegisteredHandler(ch: WebSocketChannel): Task[Unit] =
    ZIO.logInfo("Connection opened!")

  protected def channelUnregisteredHandler(ch: WebSocketChannel): Task[Unit] =
    ZIO.logInfo("Connection closed!")

  protected def handshakeCompleteHandler(ch: WebSocketChannel): Task[Unit] =
    ZIO.logInfo("Connection started!")

  protected def handshakeTimeoutHandler(ch: WebSocketChannel): Task[Unit] =
    ZIO.logInfo("Connection failed!")
}

object ProtocolHandler {
  val layer: ULayer[ProtocolHandler] = ZLayer.succeed(ProtocolHandler())
}
