package is.valsk.esper.domain

import is.valsk.esper.domain.Device
import is.valsk.esper.domain.Types.{Manufacturer, Model}

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