package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.domain.Version
import is.valsk.esper.services.FirmwareService
import zio.http.Response
import zio.http.model.{HttpError, Status}
import zio.{IO, URLayer, ZLayer}

class DownloadFirmware(
    firmwareService: FirmwareService,
) {

  def apply(manufacturer: Manufacturer, model: Model, version: Version): IO[HttpError, Response] = {
    for {
      _ <- firmwareService.getOrDownloadFirmware(manufacturer, model, version)
    } yield Response.status(Status.Ok)
  }
    .mapError(_ => HttpError.BadRequest())
}

object DownloadFirmware {

  val layer: URLayer[FirmwareService, DownloadFirmware] = ZLayer.fromFunction(DownloadFirmware(_))
}