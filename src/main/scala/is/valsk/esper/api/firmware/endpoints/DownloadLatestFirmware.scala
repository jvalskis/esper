package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.DeviceModel
import is.valsk.esper.domain.Types.{Manufacturer, ManufacturerExtractor, Model, ModelExtractor}
import is.valsk.esper.services.FirmwareDownloader
import zio.http.model.{HttpError, Status}
import zio.http.{Http, HttpApp, Request, Response}
import zio.{IO, URLayer, ZIO, ZLayer}

class DownloadLatestFirmware(
    firmwareDownloader: FirmwareDownloader,
) {

  def apply(manufacturer: Manufacturer, model: Model): IO[HttpError, Response] = (for {
      _ <- firmwareDownloader.downloadFirmware(manufacturer, model)
    } yield Response.status(Status.Ok))
      .mapError(_ => HttpError.BadRequest())
}

object DownloadLatestFirmware {

  val layer: URLayer[FirmwareDownloader, DownloadLatestFirmware] = ZLayer.fromFunction(DownloadLatestFirmware(_))
}