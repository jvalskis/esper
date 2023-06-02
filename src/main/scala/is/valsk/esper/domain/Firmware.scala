package is.valsk.esper.domain

import is.valsk.esper.domain.Types.{Manufacturer, Model}
import zio.Chunk

case class Firmware(
    manufacturer: Manufacturer,
    model: Model,
    version: Version,
    data: Array[Byte],
    size: Long,
)