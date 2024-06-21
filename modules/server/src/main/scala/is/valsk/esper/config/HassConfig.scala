package is.valsk.esper.config

import is.valsk.esper.config.Configs.makeLayer
import zio.{Config, Layer}

case class HassConfig(
    webSocketUrl: String,
    accessToken: String,
)

object HassConfig {

  val layer: Layer[Config.Error, HassConfig] = makeLayer("esper", "hass")
}