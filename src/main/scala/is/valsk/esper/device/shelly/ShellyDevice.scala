package is.valsk.esper.device.shelly

import eu.timepit.refined.api.RefType
import eu.timepit.refined.string.Url
import is.valsk.esper.device.DeviceDescriptor
import is.valsk.esper.errors.FirmwareDownloadError
import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.http.HttpClient
import is.valsk.esper.model.Device
import is.valsk.esper.model.Device.DeviceUrl
import zio.http.{Client, ClientConfig}
import zio.{IO, ULayer, URLayer, ZIO, ZLayer}

class ShellyDevice(httpClient: HttpClient) extends DeviceManufacturerHandler {

  private val hardwareAndModelRegex = "(.+) \\((.+)\\)".r

  private def getShellyFirmwareUrl(model: String) = s"http://archive.shelly-tools.de/archive.php?type=$model"

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

  override def downloadFirmware(deviceDescriptor: DeviceDescriptor): IO[FirmwareDownloadError, String] = {
    val firmwareUrl = getShellyFirmwareUrl(deviceDescriptor.model)
    for {
      _ <- ZIO.logInfo(s"Getting firmware from: $firmwareUrl")
      result <- httpClient.get(firmwareUrl)
        .flatMap(_.body.asString)
        .mapError(FirmwareDownloadError(deviceDescriptor, _))
    } yield result
  }

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

  val layer: URLayer[HttpClient, ShellyDevice] = ZLayer {
    for {
      httpClient <- ZIO.service[HttpClient]
    } yield ShellyDevice(httpClient)
  }
}
