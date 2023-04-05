package is.valsk.esper.hass

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.HassWebsocketClient
import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.*
import zio.http.socket.WebSocketChannelEvent

class HassWebsocketApp(
    channelHandler: PartialChannelHandler,
    esperConfig: EsperConfig,
) {

  def run: ZIO[Any, Throwable, Nothing] = {
    val client = Http.collectZIO[WebSocketChannelEvent](channelHandler)
      .toSocketApp
      .connect(esperConfig.hassConfig.webSocketUrl)
    (client *> ZIO.never).provide(
      Client.default,
      Scope.default,
    )
  }
}

object HassWebsocketApp {

  val layer: URLayer[EsperConfig & List[ChannelHandler], HassWebsocketApp] = ZLayer {
    for {
      esperConfig <- ZIO.service[EsperConfig]
      channelHandlers <- ZIO.service[List[ChannelHandler]]
      combinedHandler = channelHandlers.foldLeft(ChannelHandler.empty) { (a, b) => a orElse b.get }
    } yield HassWebsocketApp(combinedHandler, esperConfig)
  }
}
