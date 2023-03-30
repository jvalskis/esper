package is.valsk.esper.model

import eu.timepit.refined.api.{Refined, RefinedTypeOps}
import eu.timepit.refined.string.Url
import is.valsk.esper.model.Device.DeviceUrl

case class Device(
    id: String,
    url: DeviceUrl,
    name: String,
    nameByUser: Option[String],
    swVersion: Option[String],
    hwVersion: Option[String],
    manufacturer: String,
)

object Device {

  type DeviceUrl = String Refined Url
  object DeviceUrl extends RefinedTypeOps[DeviceUrl, String]
}