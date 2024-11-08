package is.valsk.esper

import is.valsk.esper.config.HassConfig
import is.valsk.esper.hass.protocol.ChannelHandler
import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.*

trait HassWebsocketApp {
  def run: Task[Unit]
}

object HassWebsocketApp {
  private class HassWebsocketAppLive(
      channelHandler: PartialChannelHandler,
      config: HassConfig,
  ) extends HassWebsocketApp {

    private def webSocketHandler(p: Promise[Nothing, Throwable]): ZIO[Client & Scope, Throwable, Response] = {
      Handler
        .webSocket { channel =>
          for {
            _ <- channel.receiveAll(channelHandler(channel, p, _)).catchAll {
              e => p.succeed(e) *> ZIO.fail(e)
            }
          } yield ()
        }
        .connect(config.webSocketUrl)
    }

    override def run: Task[Unit] = ZIO
      .scoped(for {
        p <- Promise.make[Nothing, Throwable]
        _ <- webSocketHandler(p)
        _ <- p.await
        _ <- ZIO.logInfo(s"Trying to reconnect...")
      } yield ())
      .provide(
        Client.default,
      ) *> run
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