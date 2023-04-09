package is.valsk.esper.repositories

import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.domain.{DeviceModel, Firmware, FirmwareDownloadError, Version}
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import zio.stream.Stream
import zio.{IO, UIO}

trait FirmwareRepository extends Repository[FirmwareKey, Firmware]

object FirmwareRepository {

  case class FirmwareKey(deviceModel: DeviceModel, version: Version)

  object FirmwareKey {
    def apply(manufacturer: Manufacturer, model: Model, version: Version): FirmwareKey =
      FirmwareKey(DeviceModel(manufacturer, model), version)
  }

}