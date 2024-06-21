package is.valsk.esper.core

import is.valsk.esper.config.BackendClientConfig
import is.valsk.esper.http.endpoints.{DeviceEndpoints, FirmwareEndpoints, OtaEndpoints}
import sttp.capabilities.WebSockets
import sttp.capabilities.zio.ZioStreams
import sttp.client3.impl.zio.FetchZioBackend
import sttp.client3.{Request, SttpBackend, UriContext}
import sttp.tapir.Endpoint
import sttp.tapir.client.sttp.SttpClientInterpreter
import zio.{Config, Layer, RLayer, Task, ULayer, URLayer, ZIO, ZLayer}

trait BackendClient {
  val devices: DeviceEndpoints
  val ota: OtaEndpoints
  val firmware: FirmwareEndpoints

  def endpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O]
}

class BackendClientLive(
    backend: SttpBackend[Task, ZioStreams & WebSockets],
    interpreter: SttpClientInterpreter,
    config: BackendClientConfig,
) extends BackendClient {
  val devices: DeviceEndpoints = new DeviceEndpoints {}
  val ota: OtaEndpoints = new OtaEndpoints {}
  val firmware: FirmwareEndpoints = new FirmwareEndpoints {}

  private def endpointRequest[I, E, O](endpoint: Endpoint[Unit, I, E, O, Any]): I => Request[Either[E, O], Any] = {
    interpreter.toRequestThrowDecodeFailures(endpoint, Some(uri"${config.uri}"))
  }

  def endpointRequestZIO[I, E <: Throwable, O](endpoint: Endpoint[Unit, I, E, O, Any])(payload: I): Task[O] = {
    backend.send(endpointRequest(endpoint)(payload)).map(_.body).absolve
  }
}

object BackendClient {
  val layer: URLayer[BackendClientConfig & SttpBackend[Task, ZioStreams with WebSockets] & SttpClientInterpreter, BackendClient] = ZLayer {
    for {
      config <- ZIO.service[BackendClientConfig]
      backend <- ZIO.service[SttpBackend[Task, ZioStreams with WebSockets]]
      interpreter <- ZIO.service[SttpClientInterpreter]
    } yield new BackendClientLive(backend, interpreter, config)
  }

  val cofiguredLayer: Layer[Config.Error, BackendClient] = {
    val backend = FetchZioBackend()
    val interpreter = SttpClientInterpreter()
    (BackendClientConfig.layer ++ ZLayer.succeed(backend) ++ ZLayer.succeed(interpreter)) >>> layer
  }
}