package is.valsk.esper.repositories

import is.valsk.esper.device.DeviceDescriptor
import is.valsk.esper.hass.device.DeviceManufacturerHandler.FirmwareDescriptor

trait FirmwareRepository extends Repository[DeviceDescriptor, FirmwareDescriptor]