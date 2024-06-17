package is.valsk.esper.config

import zio.config.magnolia
import zio.config.magnolia.{DeriveConfig, deriveConfig}
import zio.{Config, Layer, Tag, ZIO, ZLayer}

object Configs {

  def makeLayer[C](path: String, other: String*)(using dc: DeriveConfig[C], tag: Tag[C]): Layer[Config.Error, C] = {
    ZLayer.fromZIO(ZIO.config[C](deriveConfig[C].nested(path, other: _*)))
  }
}
