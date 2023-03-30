package is.valsk.esper.hass.mappers

import eu.timepit.refined.api.RefType
import eu.timepit.refined.string.Url
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.model.Device
import is.valsk.esper.model.Device.DeviceUrl

extension (hassDevice: HassResult) {

  def toDomain: Either[String, Device] = for {
    refinedUrl <- hassDevice.configuration_url.map(DeviceUrl.from).getOrElse(Left("Configuration URL is empty"))
  } yield Device(
    id = hassDevice.id,
    url = refinedUrl,
    name = hassDevice.name,
    nameByUser = hassDevice.name_by_user,
    swVersion = hassDevice.sw_version,
    hwVersion = hassDevice.hw_version,
    manufacturer = hassDevice.manufacturer,
  )
}
