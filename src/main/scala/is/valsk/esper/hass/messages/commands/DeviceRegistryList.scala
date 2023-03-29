package is.valsk.esper.hass.messages.commands

import is.valsk.esper.hass.messages.{HassMessage, Type}
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

case class DeviceRegistryList(
    `type`: String,
    id: Int
) extends HassMessage

object DeviceRegistryList {
  implicit val encoder: JsonEncoder[DeviceRegistryList] = DeriveJsonEncoder.gen[DeviceRegistryList]

  def apply(id: Int): DeviceRegistryList = DeviceRegistryList(Type.DeviceRegistryList.typeName, id)
}