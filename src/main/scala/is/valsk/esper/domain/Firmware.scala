package is.valsk.esper.domain

import zio.Chunk

case class Firmware(
    deviceModel: DeviceModel,
    version: String,
    data: Chunk[Byte],
    size: Long,
)