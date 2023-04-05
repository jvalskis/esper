package is.valsk.esper.hass.device

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.errors.{FirmwareDownloadError, FirmwareDownloadFailed}
import is.valsk.esper.hass.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.model.{Device, DeviceModel}
import is.valsk.esper.types.{Manufacturer, Model, UrlString}
import is.valsk.esper.utils.SemanticVersion
import zio.IO

trait DeviceManufacturerHandler {

  def toDomain(hassDevice: HassResult): IO[String, Device]

  def getFirmwareDownloadDetails(deviceModel: DeviceModel): IO[FirmwareDownloadError, FirmwareDescriptor]
}

object DeviceManufacturerHandler {

  case class FirmwareDescriptor(
      deviceModel: DeviceModel,
      url: UrlString,
      version: SemanticVersion
  )
}