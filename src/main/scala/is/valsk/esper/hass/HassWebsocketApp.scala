package is.valsk.esper.hass

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.HassWebsocketClient
import zio.*
import zio.http.*

object HassWebsocketApp {

  def apply(): ZIO[EsperConfig, Throwable, Nothing] = for {
    esperConfig <- ZIO.service[EsperConfig]
    client = HassWebsocketClient.hassWebsocketClient
      .provideLayer(EsperConfig.layer)
      .toSocketApp
      .connect(esperConfig.hassConfig.webSocketUrl)
    app <- (client *> ZIO.never)
      .provide(
        Client.default,
        Scope.default,
      )
  } yield app
}
