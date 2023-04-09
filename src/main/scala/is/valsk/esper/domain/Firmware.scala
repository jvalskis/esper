package is.valsk.esper.domain

import zio.Chunk

case class Firmware(
    deviceModel: DeviceModel,
    version: Version,
    data: Chunk[Byte],
    size: Long,
)