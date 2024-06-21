package is.valsk.esper.services

import is.valsk.esper.device.DeviceManufacturerHandler
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import zio.stream.ZSink
import zio.{IO, URLayer, ZIO, ZLayer}

trait FirmwareDownloader {

  def downloadFirmware(manufacturer: Manufacturer, model: Model, version: Version)(using manufacturerHandler: DeviceManufacturerHandler): IO[EsperError, Firmware]

  def downloadFirmware(firmwareDescriptor: FirmwareDescriptor): IO[EsperError, Firmware]

}

object FirmwareDownloader {
  private class FirmwareDownloaderLive(
      httpClient: HttpClient,
  ) extends FirmwareDownloader {

    override def downloadFirmware(manufacturer: Manufacturer, model: Model, version: Version)(using manufacturerHandler: DeviceManufacturerHandler): IO[EsperError, Firmware] = for {
      firmwareDetails <- manufacturerHandler.getFirmwareDownloadDetails(manufacturer, model, Some(version))
      firmware <- downloadFirmware(firmwareDetails)
    } yield firmware

    override def downloadFirmware(firmwareDescriptor: FirmwareDescriptor): IO[FirmwareDownloadError, Firmware] = for {
      bytes <- httpClient.download(firmwareDescriptor.url)
        .run(ZSink.collectAll[Byte])
        .mapError(e => FirmwareDownloadFailed(e.getMessage, firmwareDescriptor.manufacturer, firmwareDescriptor.model, Some(e)))
      firmware = Firmware(
        manufacturer = firmwareDescriptor.manufacturer,
        model = firmwareDescriptor.model,
        version = firmwareDescriptor.version,
        data = bytes.toArray,
        size = bytes.size
      )
      _ <- ZIO.logInfo(s"Firmware downloaded: $firmware")
    } yield firmware
  }

  val layer: URLayer[HttpClient, FirmwareDownloader] =
    ZLayer.fromFunction(FirmwareDownloaderLive(_))
}