package is.valsk.esper.hass.protocol.api

import is.valsk.esper.hass.messages.HassResponseMessage
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler.PartialHassResponseMessageHandler
import zio.*
import zio.http.WebSocketChannel

trait HassResponseMessageHandler {

  def get: PartialHassResponseMessageHandler
}

object HassResponseMessageHandler {

  type PartialHassResponseMessageHandler = PartialFunction[HassResponseMessageContext, Task[Unit]]

  val empty: PartialHassResponseMessageHandler = PartialFunction.empty[HassResponseMessageContext, Task[Unit]]

  case class HassResponseMessageContext(
      channel: WebSocketChannel,
      message: HassResponseMessage
  )
}