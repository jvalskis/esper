package is.valsk.esper.config

import is.valsk.esper.config.Configs.makeLayer
import zio.{Config, Layer}

case class FlywayConfig(
    url: String,
    user: String,
    password: String,
)

object FlywayConfig {

  val layer: Layer[Config.Error, FlywayConfig] = makeLayer("esper", "db", "dataSource")
}
