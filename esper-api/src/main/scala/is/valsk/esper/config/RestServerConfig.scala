package is.valsk.esper.config

import zio.{Config, Layer, ZIO, ZLayer}
import zio.config.magnolia.deriveConfig

case class RestServerConfig(
    host: String,
    port: Int = 9000,
)

object RestServerConfig {

  implicit val config: Config[RestServerConfig] =
    deriveConfig[RestServerConfig].nested("esper", "server")

  val layer: Layer[Config.Error, RestServerConfig] = ZLayer {
    ZIO.config[RestServerConfig]
  }
}