package is.valsk.esper.domain

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model, UrlString}
import zio.json.{DeriveJsonCodec, DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}

case class Device(
    id: DeviceId,
    url: UrlString,
    name: NonEmptyString,
    nameByUser: Option[String],
    model: Model,
    softwareVersion: Option[Version],
    manufacturer: Manufacturer,
)

object Device {

  import is.valsk.esper.domain.Types.NonEmptyStringImplicits.{decoder, encoder}
  import is.valsk.esper.domain.Types.UrlString.{decoder, encoder}

  given encoder: JsonEncoder[Device] = DeriveJsonEncoder.gen[Device]
  given decoder: JsonDecoder[Device] = DeriveJsonDecoder.gen[Device]
  given codec: JsonCodec[Device] = DeriveJsonCodec.gen[Device]
}