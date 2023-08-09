package is.valsk.esper.domain

import is.valsk.esper.domain.Types.{Manufacturer, Model}

case class Firmware(
    manufacturer: Manufacturer,
    model: Model,
    version: Version,
    data: Array[Byte],
    size: Long,
) {
  override def toString: String = s"Firmware($manufacturer, $model, $version, $size)"
}