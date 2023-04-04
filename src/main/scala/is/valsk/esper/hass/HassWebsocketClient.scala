package is.valsk.esper.hass

import zio.*
import zio.http.*
import zio.http.socket.{WebSocketChannelEvent}

trait HassWebsocketClient {

  def get: Http[Any, Throwable, WebSocketChannelEvent, Unit]

}