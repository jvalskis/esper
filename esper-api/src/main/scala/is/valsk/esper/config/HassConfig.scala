package is.valsk.esper.config

import zio.{Config, Layer, ZIO, ZLayer}
import zio.config.magnolia.deriveConfig

case class HassConfig(
    webSocketUrl: String,
    accessToken: String,
)

object HassConfig {

  implicit val config: Config[HassConfig] =
    deriveConfig[HassConfig].nested("esper", "hass")

  val layer: Layer[Config.Error, HassConfig] = ZLayer {
    ZIO.config[HassConfig]
  }
}