package is.valsk.esper.domain

import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.domain.Types.NonEmptyStringImplicits.*
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class Device(
    id: NonEmptyString,
    url: UrlString,
    name: NonEmptyString,
    nameByUser: Option[String],
    model: Model,
    softwareVersion: Option[String],
    manufacturer: Manufacturer,
)

object Device {

  import is.valsk.esper.domain.Types.UrlString.encoder
  import is.valsk.esper.domain.Types.NonEmptyStringImplicits.encoder
  import is.valsk.esper.domain.Types.UrlString.decoder
  import is.valsk.esper.domain.Types.NonEmptyStringImplicits.decoder

  implicit val encoder: JsonEncoder[Device] = DeriveJsonEncoder.gen[Device]
  implicit val decoder: JsonDecoder[Device] = DeriveJsonDecoder.gen[Device]
}