package is.valsk.esper.hass.messages.commands

import is.valsk.esper.hass.messages.{HassIdentifiableMessage, HassRequestMessage, Type}
import zio.json.{DeriveJsonEncoder, JsonEncoder}

case class DeviceRegistryList(
    `type`: String,
    id: Int
) extends HassRequestMessage with HassIdentifiableMessage

object DeviceRegistryList {
  given encoder: JsonEncoder[DeviceRegistryList] = DeriveJsonEncoder.gen[DeviceRegistryList]

  def apply(id: Int): DeviceRegistryList = DeviceRegistryList(Type.DeviceRegistryList.typeName, id)
}