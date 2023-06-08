package is.valsk.esper.device

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
import is.valsk.esper.hass.messages.responses.HassResult
import zio.IO

trait DeviceManufacturerHandler {

  def getFirmwareDownloadDetails(
      manufacturer: Manufacturer,
      model: Model,
      version: Option[Version]
  ): IO[FirmwareDownloadError, FirmwareDescriptor]

  def versionOrdering: Ordering[Version]
}

object DeviceManufacturerHandler {

  case class FirmwareDescriptor(
      manufacturer: Manufacturer,
      model: Model,
      url: UrlString,
      version: Version
  )
}