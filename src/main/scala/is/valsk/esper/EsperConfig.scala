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

  def port: RIO[EsperConfig, Int] = ZIO.serviceWith[EsperConfig](_.port)

  def hassConfig: RIO[EsperConfig, HassConfig] = ZIO.serviceWith[EsperConfig](_.hassConfig)

  def scheduleConfig: RIO[EsperConfig, ScheduleConfig] = ZIO.serviceWith[EsperConfig](_.scheduleConfig)
}