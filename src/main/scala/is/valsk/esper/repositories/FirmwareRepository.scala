package is.valsk.esper.repositories

import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.DeviceModel

trait FirmwareRepository extends Repository[DeviceModel, FirmwareDescriptor]