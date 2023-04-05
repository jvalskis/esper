package is.valsk.esper.model

import is.valsk.esper.model.Device
import is.valsk.esper.types.{Manufacturer, Model}

case class DeviceModel(
    manufacturer: Manufacturer,
    model: Model
)

object DeviceModel {

  def apply(manufacturer: String, model: String): Either[String, DeviceModel] = for {
    manufacturerRefined <- Manufacturer.from(manufacturer)
    modelRefined <- Model.from(model)
  } yield DeviceModel(
    manufacturerRefined,
    modelRefined
  )
}