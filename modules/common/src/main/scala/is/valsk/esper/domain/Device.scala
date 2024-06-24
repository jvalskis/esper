package is.valsk.esper.domain

import is.valsk.esper.domain.Types.*
import zio.json.{DeriveJsonCodec, DeriveJsonDecoder, DeriveJsonEncoder, JsonCodec, JsonDecoder, JsonEncoder}

case class Device(
    id: DeviceId,
    url: UrlString,
    name: Name,
    nameByUser: Option[String],
    model: Model,
    softwareVersion: Option[Version],
    manufacturer: Manufacturer,
)

object Device {

  import is.valsk.esper.domain.Types.UrlString.{decoder, encoder}

  given encoder: JsonEncoder[Device] = DeriveJsonEncoder.gen[Device]
  given decoder: JsonDecoder[Device] = DeriveJsonDecoder.gen[Device]
  given codec: JsonCodec[Device] = DeriveJsonCodec.gen[Device]
}