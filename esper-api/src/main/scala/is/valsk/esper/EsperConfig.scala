package is.valsk.esper

import zio.*
import zio.config.*
import zio.config.magnolia.deriveConfig

case class EsperConfig(
    host: String,
    port: Int = 9000,
    hassConfig: HassConfig,
    scheduleConfig: ScheduleConfig,
)

case class HassConfig(
    webSocketUrl: String,
    accessToken: String,
)

case class ScheduleConfig(
    initialDelay: Int,
    interval: Int,
    jitter: Boolean,
    maxRetries: Int,
    exponentialRetryBase: Int
)

object EsperConfig {

  implicit val config: Config[EsperConfig] =
    deriveConfig[EsperConfig].nested("EsperConfig")

  val layer: ZLayer[Any, Config.Error, EsperConfig] = ZLayer {
    for {
      esperConfig <- ZIO.config[EsperConfig]
    } yield esperConfig
  }

  val scheduleConfigLayer: ZLayer[EsperConfig, Nothing, ScheduleConfig] = ZLayer {
    for {
      esperConfig <- ZIO.service[EsperConfig]
    } yield esperConfig.scheduleConfig
  }
}