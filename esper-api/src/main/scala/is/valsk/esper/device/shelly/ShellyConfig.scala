package is.valsk.esper.device.shelly

import zio.ZLayer
import zio.config.magnolia.descriptor
import zio.config.typesafe.TypesafeConfigSource
import zio.config.{PropertyTreePath, ReadError, read}

case class ShellyConfig(
    firmwareListUrlPattern: String,
    firmwareDownloadUrlPattern: String,
    firmwareFlashTimeout: Int,
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