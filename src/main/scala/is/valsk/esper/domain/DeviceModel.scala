package is.valsk.esper.domain

import is.valsk.esper.domain.Device
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class DeviceModel(
    manufacturer: Manufacturer,
    model: Model
)

object DeviceModel {
  import is.valsk.esper.domain.Types.NonEmptyStringImplicits.{decoder, encoder}

  implicit val encoder: JsonEncoder[DeviceModel] = DeriveJsonEncoder.gen[DeviceModel]
  implicit val decoder: JsonDecoder[DeviceModel] = DeriveJsonDecoder.gen[DeviceModel]

  def apply(manufacturer: String, model: String): Either[String, DeviceModel] = for {
    manufacturerRefined <- Manufacturer.from(manufacturer)
    modelRefined <- Model.from(model)
  } yield DeviceModel(
    manufacturerRefined,
    modelRefined
  )

  def apply(firmware: Firmware): DeviceModel = DeviceModel(firmware.manufacturer, firmware.model)

  def unapply(firmware: Firmware): Option[(Manufacturer, Model)] = Some((firmware.manufacturer, firmware.model))
}