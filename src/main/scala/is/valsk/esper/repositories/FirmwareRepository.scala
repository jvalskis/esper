package is.valsk.esper.repositories

import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.{DeviceModel, Firmware, FirmwareDownloadError}
import zio.stream.Stream
import zio.{IO, UIO}

trait FirmwareRepository extends Repository[DeviceModel, Firmware]