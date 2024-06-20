package is.valsk.esper.config

import is.valsk.esper.config.Configs.makeLayer
import zio.{Config, Layer}

case class HttpServerConfig(
    host: String,
    port: Int = 9000,
)

object HttpServerConfig {

  val layer: Layer[Config.Error, HttpServerConfig] = makeLayer("esper", "server")
}