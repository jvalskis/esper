package is.valsk.esper.hass.device

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.hass.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.domain.{Device, DeviceModel, FirmwareDownloadError, FirmwareDownloadFailed, SemanticVersion}
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
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