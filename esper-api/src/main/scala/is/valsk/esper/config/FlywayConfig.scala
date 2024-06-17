package is.valsk.esper.config

import zio.{Config, Layer, ZIO, ZLayer}
import zio.config.magnolia.deriveConfig

case class FlywayConfig(
    url: String,
    user: String,
    password: String,
)

object FlywayConfig {

  implicit val config: Config[FlywayConfig] =
    deriveConfig[FlywayConfig].nested("esper", "db", "dataSource")

  val layer: Layer[Config.Error, FlywayConfig] = ZLayer {
    ZIO.config[FlywayConfig]
  }
}
