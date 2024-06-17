package is.valsk.esper.config

import is.valsk.esper.config.Configs.makeLayer
import zio.{Config, Layer}

case class ScheduleConfig(
    initialDelay: Int,
    interval: Int,
    jitter: Boolean,
    maxRetries: Int,
    exponentialRetryBase: Int
)

object ScheduleConfig {

  val layer: Layer[Config.Error, ScheduleConfig] = makeLayer("esper", "schedule")
}