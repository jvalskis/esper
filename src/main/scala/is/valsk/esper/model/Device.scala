package is.valsk.esper.model

import eu.timepit.refined.api.{Refined, RefinedTypeOps}
import eu.timepit.refined.string.Url
import is.valsk.esper.model.Device.DeviceUrl
import zio.json.{DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class Device(
    id: String,
    url: DeviceUrl,
    name: String,
    nameByUser: Option[String],
    model: String,
    hardware: Option[String],
    softwareVersion: Option[String],
    manufacturer: String,
)

object Device {
  import DeviceUrl.*
  implicit val encoder: JsonEncoder[Device] = DeriveJsonEncoder.gen[Device]

  type DeviceUrl = String Refined Url

  object DeviceUrl extends RefinedTypeOps[DeviceUrl, String] {
    implicit val encoder: JsonEncoder[DeviceUrl] = JsonEncoder[String].contramap(_.toString)
    implicit val decoder: JsonDecoder[DeviceUrl] = JsonDecoder[String].mapOrFail(DeviceUrl.from)
  }
}