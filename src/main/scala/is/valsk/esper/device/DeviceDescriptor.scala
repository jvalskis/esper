package is.valsk.esper.device

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.model.Device
import is.valsk.esper.types.Manufacturer

case class DeviceDescriptor(
    manufacturer: Manufacturer,
    hardware: Option[String],
    model: NonEmptyString
)

object DeviceDescriptor {

  def apply(manufacturer: String, hardware: Option[String], model: String): Either[String, DeviceDescriptor] = for {
    manufacturerRefined <- Manufacturer.from(manufacturer)
    modelRefined <- NonEmptyString.from(model)
  } yield DeviceDescriptor(
    manufacturerRefined,
    hardware,
    modelRefined
  )

  def unapply(device: Device): Option[DeviceDescriptor] = Some(DeviceDescriptor(
    device.manufacturer,
    device.hardware,
    device.model
  ))
}