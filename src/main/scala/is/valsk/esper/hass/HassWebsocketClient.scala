package is.valsk.esper.hass

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.messages.MessageIdGenerator
import is.valsk.esper.hass.messages.commands.DeviceRegistryList
import is.valsk.esper.hass.messages.responses.Result
import is.valsk.esper.model.Device
import is.valsk.esper.services.Repository
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

trait HassWebsocketClient {

  def get: Http[Any, Throwable, WebSocketChannelEvent, Unit]

}