package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.services.FirmwareService
import zio.http.Response
import zio.http.model.{HttpError, Status}
import zio.{IO, URLayer, ZLayer}

class DownloadLatestFirmware(
    firmwareService: FirmwareService,
) {

  def apply(manufacturer: Manufacturer, model: Model): IO[HttpError, Response] = (for {
      _ <- firmwareService.getOrDownloadLatestFirmware(manufacturer, model)
    } yield Response.status(Status.Ok))
      .mapError(_ => HttpError.BadRequest())
}

object DownloadLatestFirmware {

  val layer: URLayer[FirmwareService, DownloadLatestFirmware] = ZLayer.fromFunction(DownloadLatestFirmware(_))
}