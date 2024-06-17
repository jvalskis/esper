package is.valsk.esper.config

import is.valsk.esper.config.Configs.makeLayer
import zio.{Config, Layer}

case class RestServerConfig(
    host: String,
    port: Int = 9000,
)

object RestServerConfig {

  val layer: Layer[Config.Error, RestServerConfig] = makeLayer("esper", "server")
}