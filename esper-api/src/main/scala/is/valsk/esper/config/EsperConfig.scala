package is.valsk.esper.config

import zio.{Config, Layer, ZIO, ZLayer}
import zio.config.magnolia.deriveConfig

case class EsperConfig(
    server: RestServerConfig,
    hass: HassConfig,
    schedule: ScheduleConfig,
)

object EsperConfig {

  implicit val config: Config[EsperConfig] =
    deriveConfig[EsperConfig].nested("esper")

  val layer: Layer[Config.Error, EsperConfig] = ZLayer {
    for {
      esperConfig <- ZIO.config[EsperConfig]
    } yield esperConfig
  }
}