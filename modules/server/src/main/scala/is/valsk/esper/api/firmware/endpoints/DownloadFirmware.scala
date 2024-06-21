package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.domain.{EsperError, Version}
import is.valsk.esper.services.FirmwareService
import zio.{IO, URLayer, ZLayer}

class DownloadFirmware(
    firmwareService: FirmwareService,
) {

  def apply(manufacturer: Manufacturer, model: Model, version: Version): IO[EsperError, Unit] =
    firmwareService.getOrDownloadFirmware(manufacturer, model, version).map(_ => ())
}

object DownloadFirmware {

  val layer: URLayer[FirmwareService, DownloadFirmware] = ZLayer.fromFunction(DownloadFirmware(_))
}