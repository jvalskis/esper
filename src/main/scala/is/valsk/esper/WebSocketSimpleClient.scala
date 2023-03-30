package is.valsk.esper

import is.valsk.esper.hass.HassWebsocketClient
import zio.*
import zio.http.*

object WebSocketSimpleClient extends ZIOAppDefault {

  private val app = ZIO.service[EsperConfig].flatMap(
    config => HassWebsocketClient.hassWebsocketClient
      .provideLayer(EsperConfig.layer)
      .toSocketApp
      .connect(config.hassConfig.webSocketUrl) *> ZIO.never
  )

  val run: ZIO[Any, Throwable, Nothing] = app.provide(
    Client.default,
    Scope.default,
    EsperConfig.layer
  )

}