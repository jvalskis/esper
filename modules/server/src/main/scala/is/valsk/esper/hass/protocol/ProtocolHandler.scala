package is.valsk.esper.hass.protocol

import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.*

class ProtocolHandler extends ChannelHandler {

  override def get: PartialChannelHandler = {
    case (channel, _, UserEventTriggered(event)) =>
      event match {
        case HandshakeComplete => handshakeCompleteHandler(channel)
        case HandshakeTimeout => handshakeTimeoutHandler(channel)
      }

    case (channel, promise, Unregistered) =>
      channelUnregisteredHandler(channel, promise)

    case (channel, _, Read(WebSocketFrame.Pong)) =>
      pingHandler(channel)
  }

  protected def pingHandler(ch: WebSocketChannel): Task[Unit] = {
    for {
      _ <- ZIO.logInfo("Received PONG - sending PING")
      _ <- ZIO.sleep(5.seconds)
      _ <- ch.send(Read(WebSocketFrame.Ping))
    } yield ()
  }

  protected def channelUnregisteredHandler(ch: WebSocketChannel, promise: Promise[Nothing, Throwable]): Task[Unit] = for {
    _ <- ZIO.logError("Connection closed2!")
    _ <- ZIO.fail(new RuntimeException("Connection closed3!"))
  } yield ()

  protected def handshakeCompleteHandler(ch: WebSocketChannel): Task[Unit] = for {
    _ <- ZIO.logInfo("Connection started!")
    _ <- ch.send(Read(WebSocketFrame.Ping))
  } yield ()

  protected def handshakeTimeoutHandler(ch: WebSocketChannel): Task[Unit] =
    ZIO.logInfo("Connection failed!")
}

object ProtocolHandler {
  val layer: ULayer[ProtocolHandler] = ZLayer.succeed(ProtocolHandler())
}
