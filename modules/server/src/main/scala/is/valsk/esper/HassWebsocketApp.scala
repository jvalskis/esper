package is.valsk.esper

import is.valsk.esper.config.HassConfig
import is.valsk.esper.hass.protocol.ChannelHandler
import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.*

trait HassWebsocketApp {
  def run: Task[Nothing]
}

object HassWebsocketApp {
  private class HassWebsocketAppLive(
      channelHandler: PartialChannelHandler,
      config: HassConfig,
  ) extends HassWebsocketApp {

    def run: Task[Nothing] = {
      val client = Handler
        .webSocket {
          channel => channel.receiveAll(event => channelHandler(channel, event))
        }
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
