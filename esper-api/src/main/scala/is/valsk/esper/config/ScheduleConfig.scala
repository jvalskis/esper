package is.valsk.esper.config

import zio.{Config, Layer, ZIO, ZLayer}
import zio.config.magnolia.deriveConfig

case class ScheduleConfig(
    initialDelay: Int,
    interval: Int,
    jitter: Boolean,
    maxRetries: Int,
    exponentialRetryBase: Int
)

object ScheduleConfig {

  implicit val config: Config[ScheduleConfig] =
    deriveConfig[ScheduleConfig].nested("esper", "schedule")

  val layer: Layer[Config.Error, ScheduleConfig] = ZLayer {
    ZIO.config[ScheduleConfig]
  }
}