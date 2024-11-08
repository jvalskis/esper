package is.valsk.esper.hass.protocol

import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.*
import zio.http.WebSocketChannelEvent

trait ChannelHandler {
  def get: PartialChannelHandler
}

object ChannelHandler {

  type PartialChannelHandler = PartialFunction[(WebSocketChannel, Promise[Nothing, Throwable], WebSocketChannelEvent), Task[Unit]]

  val empty: PartialChannelHandler = PartialFunction.empty[(WebSocketChannel, Promise[Nothing, Throwable], WebSocketChannelEvent), Task[Unit]]
}