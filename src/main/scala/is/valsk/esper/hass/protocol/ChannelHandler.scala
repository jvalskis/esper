package is.valsk.esper.hass.protocol

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.{AuthInvalid, AuthOK, AuthRequired, Result}
import is.valsk.esper.hass.messages.{HassResponseMessage, MessageIdGenerator}
import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import is.valsk.esper.services.Repository
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

trait ChannelHandler {
  def get: PartialChannelHandler
}

object ChannelHandler {

  type PartialChannelHandler = PartialFunction[WebSocketChannelEvent, Task[Unit]]

  val empty: PartialChannelHandler = PartialFunction.empty[WebSocketChannelEvent, Task[Unit]]
}