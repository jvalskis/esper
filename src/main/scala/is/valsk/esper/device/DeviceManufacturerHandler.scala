package is.valsk.esper.device

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
import is.valsk.esper.hass.messages.responses.HassResult
import zio.IO

trait DeviceManufacturerHandler {

  def getFirmwareDownloadDetails(deviceModel: DeviceModel): IO[FirmwareDownloadError, FirmwareDescriptor]
}

object DeviceManufacturerHandler {

  case class FirmwareDescriptor(
      deviceModel: DeviceModel,
      url: UrlString,
      version: SemanticVersion
  )
}