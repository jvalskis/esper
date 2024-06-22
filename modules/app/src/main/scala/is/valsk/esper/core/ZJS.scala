package is.valsk.esper.core

import com.raquo.airstream.eventbus.EventBus
import com.raquo.airstream.state.Var
import sttp.tapir.Endpoint
import zio.{Runtime, Task, Unsafe, ZIO}

object ZJS {

  def useBackend[A]: ZIO.ServiceWithZIOPartiallyApplied[BackendClient] = {
    ZIO.serviceWithZIO[BackendClient]
  }

  extension [E <: Throwable, A](zio: ZIO[BackendClient, E, A]) {
    def emitTo(eventBus: EventBus[A]): Unit = {
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.fork(
          zio
            .tap(value => ZIO.attempt(eventBus.emit(value)))
            .provide(BackendClient.cofiguredLayer)
        )
      }
      ()
    }

    def setTo(`var`: Var[A]): Unit = {
      Unsafe.unsafe { implicit unsafe =>
        Runtime.default.unsafe.fork(
          zio
            .tap(value => ZIO.attempt(`var`.set(value)))
            .provide(BackendClient.cofiguredLayer)
        )
      }
      ()
    }
  }

  extension [I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any]) {
    def apply(payload: I): Task[O] = ZIO
      .service[BackendClient]
      .flatMap(_.endpointRequestZIO(endpoint)(payload))
      .provide(BackendClient.cofiguredLayer)
  }
}
