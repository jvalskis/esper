package is.valsk.esper.hass

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.protocol.ChannelHandler
import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.*
import zio.http.socket.WebSocketChannelEvent

trait HassWebsocketApp {
  def run: Task[Nothing]
}

object HassWebsocketApp {
  private class HassWebsocketAppLive(
      channelHandler: PartialChannelHandler,
      esperConfig: EsperConfig,
  ) extends HassWebsocketApp {

    def run: Task[Nothing] = {
      val client = Http.collectZIO[WebSocketChannelEvent](channelHandler)
        .toSocketApp
        .connect(esperConfig.hassConfig.webSocketUrl)
      (client *> ZIO.never).provide(
        Client.default,
        Scope.default,
      )
    }
  }

  val layer: URLayer[EsperConfig & List[ChannelHandler], HassWebsocketApp] = ZLayer {
    for {
      esperConfig <- ZIO.service[EsperConfig]
      channelHandlers <- ZIO.service[List[ChannelHandler]]
      combinedHandler = channelHandlers.foldLeft(ChannelHandler.empty) { (a, b) => a orElse b.get }
    } yield HassWebsocketAppLive(combinedHandler, esperConfig)
  }
}
