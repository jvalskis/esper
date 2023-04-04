package is.valsk.esper.device

import is.valsk.esper.model.Device

case class DeviceDescriptor(
    manufacturer: String,
    hardware: Option[String],
    model: String
)

object DeviceDescriptor {

  def unapply(device: Device): Option[DeviceDescriptor] = Some(DeviceDescriptor(
    device.manufacturer,
    device.hardware,
    device.model
  ))
}