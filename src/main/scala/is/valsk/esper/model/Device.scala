package is.valsk.esper.model

import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.types.NonEmptyStringImplicits.*
import is.valsk.esper.types.{Manufacturer, Model, UrlString}
import zio.json.{DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class Device(
    id: NonEmptyString,
    url: UrlString,
    name: NonEmptyString,
    nameByUser: Option[String],
    model: Model,
    hardware: Option[String],
    softwareVersion: Option[String],
    manufacturer: Manufacturer,
)

object Device {

  import UrlString.encoder
  import is.valsk.esper.types.NonEmptyStringImplicits.encoder

  implicit val encoder: JsonEncoder[Device] = DeriveJsonEncoder.gen[Device]
}