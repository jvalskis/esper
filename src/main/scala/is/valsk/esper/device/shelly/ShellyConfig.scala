package is.valsk.esper.device.shelly

import is.valsk.esper.{EsperConfig, HassConfig}
import zio.config.magnolia.descriptor
import zio.{RIO, ZIO, ZLayer}
import zio.config.{PropertyTreePath, ReadError, read}
import zio.config.typesafe.TypesafeConfigSource

case class ShellyConfig(
    firmwareListUrlPattern: String,
)

object ShellyConfig {
  val layer: ZLayer[Any, ReadError[String], ShellyConfig] = ZLayer {
    read {
      descriptor[ShellyConfig].from(
        TypesafeConfigSource.fromResourcePath
          .at(PropertyTreePath.$("ShellyConfig"))
      )
    }
  }
}