package is.valsk.esper.device.shelly

import eu.timepit.refined.api.RefType
import eu.timepit.refined.string.Url
import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.model.Device
import is.valsk.esper.model.Device.DeviceUrl
import zio.{IO, ULayer, ZIO, ZLayer}

class ShellyDevice extends DeviceManufacturerHandler {

  private val hardwareAndModelRegex = "(.+) \\((.+)\\)".r

  override def toDomain(hassDevice: HassResult): IO[String, Device] = ZIO.fromEither(
    for {
      hardwareModel <- resolveHardwareModel(hassDevice)
      (hardware, model) = hardwareModel
      refinedUrl <- hassDevice.configuration_url.map(DeviceUrl.from).getOrElse(Left("Configuration URL is empty"))
    } yield Device(
      id = hassDevice.id,
      url = refinedUrl,
      name = hassDevice.name,
      nameByUser = hassDevice.name_by_user,
      softwareVersion = hassDevice.sw_version,
      hardware = Some(hardware),
      model = model,
      manufacturer = hassDevice.manufacturer,
    )
  )

  private def resolveHardwareModel(hassDevice: HassResult) = {
    hassDevice.hw_version
      .flatMap {
        case hardwareAndModelRegex(hardware, model) => Some((hardware, model))
        case _ => None
      }
      .map(Right(_))
      .getOrElse(Left("Invalid hw_version format"))
  }
}

object ShellyDevice {

  def layer: ULayer[ShellyDevice] = ZLayer.succeed(ShellyDevice())
}
