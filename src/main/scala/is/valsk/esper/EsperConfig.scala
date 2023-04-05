package is.valsk.esper

import zio.*
import zio.config.*
import zio.config.magnolia.descriptor
import zio.config.typesafe.TypesafeConfigSource
import zio.http.Response

case class EsperConfig(
    port: Int = 9000,
    firmwareStoragePath: String,
    hassConfig: HassConfig,
)

case class HassConfig(
    webSocketUrl: String,
    accessToken: String,
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
}