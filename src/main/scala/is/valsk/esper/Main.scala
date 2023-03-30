package is.valsk.esper

import is.valsk.esper.api.ApiServerApp
import is.valsk.esper.hass.HassWebsocketApp
import zio.*
import zio.http.*
import zio.stream.ZStream

object Main extends ZIOAppDefault {

  private val scopedApp: ZIO[Any, Throwable, Unit] = for {
    _ <- ZStream
      .mergeAllUnbounded(16)(
        ZStream.fromZIO(HassWebsocketApp().provideLayer(EsperConfig.layer)),
        ZStream.fromZIO(ApiServerApp().provideLayer(EsperConfig.layer))
      )
      .runDrain
  } yield ()

  override val run: URIO[Any, ExitCode] = ZIO.scoped(scopedApp).exitCode
}