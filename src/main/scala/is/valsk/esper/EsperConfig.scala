package is.valsk.esper

import zio.*
import zio.config.*
import zio.config.magnolia.descriptor
import zio.config.typesafe.TypesafeConfigSource
import zio.http.Response

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
  val layer: ZLayer[Any, ReadError[String], EsperConfig] = ZLayer {
    read {
      descriptor[EsperConfig].from(
        TypesafeConfigSource.fromResourcePath
          .at(PropertyTreePath.$("EsperConfig"))
      )
    }
  }

  val scheduleConfigLayer: ZLayer[EsperConfig, Nothing, ScheduleConfig] = ZLayer {
    for {
      esperConfig <- ZIO.service[EsperConfig]
    } yield esperConfig.scheduleConfig
  }
}