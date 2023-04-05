package is.valsk.esper.repositories

import is.valsk.esper.hass.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.model.DeviceModel

trait FirmwareRepository extends Repository[DeviceModel, FirmwareDescriptor]