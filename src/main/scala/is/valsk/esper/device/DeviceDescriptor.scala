package is.valsk.esper.device

import is.valsk.esper.model.Device
import is.valsk.esper.types.{Manufacturer, Model}

case class DeviceDescriptor(
    manufacturer: Manufacturer,
    model: Model
)

object DeviceDescriptor {

  def apply(manufacturer: String, model: String): Either[String, DeviceDescriptor] = for {
    manufacturerRefined <- Manufacturer.from(manufacturer)
    modelRefined <- Model.from(model)
  } yield DeviceDescriptor(
    manufacturerRefined,
    modelRefined
  )
}