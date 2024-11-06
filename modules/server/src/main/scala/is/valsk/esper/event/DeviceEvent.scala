package is.valsk.esper.event

import is.valsk.esper.domain.Device

sealed trait DeviceEvent

case class DeviceAdded(device: Device) extends DeviceEvent

case class DeviceUpdated(device: Device) extends DeviceEvent

case class DeviceRemoved(device: Device) extends DeviceEvent

