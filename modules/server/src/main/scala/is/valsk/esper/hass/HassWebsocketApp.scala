package is.valsk.esper.hass

import is.valsk.esper.config.HassConfig
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
      config: HassConfig,
  ) extends HassWebsocketApp {

    def run: Task[Nothing] = {
      val client = Http.collectZIO[WebSocketChannelEvent](channelHandler)
        .toSocketApp
        .connect(config.webSocketUrl)
      for {
        _ <- ZIO.logInfo(s"Connecting to HASS @ ${config.webSocketUrl}")
        result <- (client *> ZIO.never)
          .provide(
            Client.default,
            Scope.default,
          )
          .logError("Error connecting to HASS")
      } yield result
    }
  }

  val layer: URLayer[HassConfig & List[ChannelHandler], HassWebsocketApp] = ZLayer {
    for {
      config <- ZIO.service[HassConfig]
      channelHandlers <- ZIO.service[List[ChannelHandler]]
      combinedHandler = channelHandlers.foldLeft(ChannelHandler.empty) { (a, b) => a orElse b.get }
    } yield HassWebsocketAppLive(combinedHandler, config)
  }

  val configuredLayer: RLayer[List[ChannelHandler], HassWebsocketApp] = HassConfig.layer >>> layer
}
