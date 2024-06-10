package is.valsk.esper.device.shelly

import zio.*
import zio.config.*
import zio.config.magnolia.deriveConfig

case class ShellyConfig(
    firmwareListUrlPattern: String,
    firmwareDownloadUrlPattern: String,
    firmwareFlashTimeout: Int,
)

object ShellyConfig {

  implicit val config: Config[ShellyConfig] =
    deriveConfig[ShellyConfig].nested("ShellyConfig")

  val layer: ZLayer[Any, Config.Error, ShellyConfig] = ZLayer {
    for {
      shellyConfig <- ZIO.config[ShellyConfig]
    } yield shellyConfig
  }
}