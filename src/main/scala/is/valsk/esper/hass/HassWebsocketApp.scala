package is.valsk.esper.hass

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.HassWebsocketClient
import zio.*
import zio.http.*

class HassWebsocketApp(
    hassWebsocketClient: HassWebsocketClient,
    esperConfig: EsperConfig,
) {

  def run: ZIO[Any, Throwable, Nothing] = {
    val client = hassWebsocketClient
      .get
      .toSocketApp
      .connect(esperConfig.hassConfig.webSocketUrl)
    (client *> ZIO.never).provide(
      Client.default,
      Scope.default,
    )
  }
}

object HassWebsocketApp {

  val layer: URLayer[EsperConfig & HassWebsocketClient, HassWebsocketApp] = ZLayer {
    for {
      esperConfig <- ZIO.service[EsperConfig]
      hassWebsocketClient <- ZIO.service[HassWebsocketClient]
    } yield HassWebsocketApp(hassWebsocketClient, esperConfig)
  }

}
