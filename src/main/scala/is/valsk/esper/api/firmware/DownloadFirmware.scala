package is.valsk.esper.api.firmware

import is.valsk.esper.api.FirmwareApi
import is.valsk.esper.domain.DeviceModel
import is.valsk.esper.domain.Types.{Manufacturer, ManufacturerExtractor, Model, ModelExtractor}
import is.valsk.esper.services.FirmwareDownloader
import zio.http.model.Status
import zio.http.{Http, HttpApp, Request, Response}
import zio.{IO, URLayer, ZIO, ZLayer}

class DownloadFirmware(
    firmwareDownloader: FirmwareDownloader,
) {

  def apply(manufacturer: Manufacturer, model: Model): IO[Response, Response] = (for {
      _ <- firmwareDownloader.downloadFirmware(DeviceModel(manufacturer, model))
    } yield Response.status(Status.Ok))
      .mapError(_ => Response.status(Status.BadRequest))
}

object DownloadFirmware {

  val layer: URLayer[FirmwareDownloader, DownloadFirmware] = ZLayer {
    for {
      firmwareDownloader <- ZIO.service[FirmwareDownloader]
    } yield DownloadFirmware(firmwareDownloader)
  }
}