package is.valsk.esper.repositories

import is.valsk.esper.hass.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.DeviceModel

trait FirmwareRepository extends Repository[DeviceModel, FirmwareDescriptor]