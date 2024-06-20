package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.EsperError
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.services.FirmwareService
import zio.{IO, URLayer, ZLayer}

class DownloadLatestFirmware(
    firmwareService: FirmwareService,
) {

  def apply(manufacturer: Manufacturer, model: Model): IO[EsperError, Unit] =
    firmwareService.getOrDownloadLatestFirmware(manufacturer, model).map(_ => ())
}

object DownloadLatestFirmware {

  val layer: URLayer[FirmwareService, DownloadLatestFirmware] = ZLayer.fromFunction(DownloadLatestFirmware(_))
}