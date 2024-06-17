package is.valsk.esper.hass

import is.valsk.esper.config.EsperConfig
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
        .connect(esperConfig.hass.webSocketUrl)
      for {
        _ <- ZIO.logInfo(s"Connecting to HASS @ ${esperConfig.hass.webSocketUrl}")
        result <- (client *> ZIO.never)
          .provide(
            Client.default,
            Scope.default,
          )
          .logError("Error connecting to HASS")
      } yield result
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
