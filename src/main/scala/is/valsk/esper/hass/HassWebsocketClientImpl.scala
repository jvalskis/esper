package is.valsk.esper.hass

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.protocol.ChannelHandler
import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

class HassWebsocketClientImpl(
    channelHandler: PartialChannelHandler
) extends HassWebsocketClient {

  override def get: Http[Any, Throwable, WebSocketChannelEvent, Unit] = Http.collectZIO[WebSocketChannelEvent](channelHandler)
}

object HassWebsocketClientImpl {
  val layer: URLayer[List[ChannelHandler], HassWebsocketClient] = ZLayer {
    for {
      channelHandlers <- ZIO.service[List[ChannelHandler]]
      combinedHandler = channelHandlers.foldLeft(ChannelHandler.empty) { (a, b) => a orElse b.get }
    } yield HassWebsocketClientImpl(combinedHandler)
  }
}