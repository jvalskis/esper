package is.valsk.esper.domain

import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.domain.Types.NonEmptyStringImplicits.*
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
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

  import is.valsk.esper.domain.Types.UrlString.encoder
  import is.valsk.esper.domain.Types.NonEmptyStringImplicits.encoder

  implicit val encoder: JsonEncoder[Device] = DeriveJsonEncoder.gen[Device]
}