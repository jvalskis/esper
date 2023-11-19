package is.valsk.esper.hass.protocol

import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.*
import zio.http.socket.WebSocketChannelEvent

trait ChannelHandler {
  def get: PartialChannelHandler
}

object ChannelHandler {

  type PartialChannelHandler = PartialFunction[WebSocketChannelEvent, Task[Unit]]

  val empty: PartialChannelHandler = PartialFunction.empty[WebSocketChannelEvent, Task[Unit]]
}