package is.valsk.esper.config

import zio.{Config, ZLayer}

case class BackendClientConfig(
    uri: String
)

object BackendClientConfig {

  val layer: ZLayer[Any, Config.Error, BackendClientConfig] = ZLayer.succeed(BackendClientConfig("http://localhost:9000"))// TODO implement proper layer when zio-config is available on scalajs + scala3
}