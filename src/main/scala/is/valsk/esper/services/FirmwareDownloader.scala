package is.valsk.esper.services

import is.valsk.esper.EsperConfig
import is.valsk.esper.device.DeviceManufacturerHandler
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
import is.valsk.esper.hass.HassToDomainMapper
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import zio.nio.file.{Files, Path}
import zio.stream.{ZChannel, ZSink, ZStream}
import zio.{IO, ULayer, URLayer, ZIO, ZLayer}

trait FirmwareDownloader {

  def downloadFirmware(manufacturer: Manufacturer, model: Model, version: Option[Version] = None): IO[EsperError, Firmware]

}

object FirmwareDownloader {
  private class FirmwareDownloaderLive(
      manufacturerRegistry: ManufacturerRepository,
      firmwareRepository: FirmwareRepository,
      httpClient: HttpClient,
      pendingUpdateService: PendingUpdateService
  ) extends FirmwareDownloader {

    def downloadFirmware(manufacturer: Manufacturer, model: Model, maybeVersion: Option[Version]): IO[EsperError, Firmware] = for {
      manufacturerHandler <- manufacturerRegistry.get(manufacturer)
      firmware <- maybeVersion match {
        case Some(version) => downloadSpecificVersion(manufacturer, model, version)(using manufacturerHandler)
        case None => downloadLatestVersion(manufacturer, model, maybeVersion)(using manufacturerHandler)
      }
    } yield firmware

    private def downloadLatestVersion(manufacturer: Manufacturer, model: Model, maybeVersion: Option[Version])(using manufacturerHandler: DeviceManufacturerHandler) = {
      for {
        _ <- ZIO.logInfo(s"Downloading latest version for $manufacturer $model")
        firmwareDetails <- manufacturerHandler.getFirmwareDownloadDetails(manufacturer, model, maybeVersion)
        firmware <- downloadIfNecessary(FirmwareKey(firmwareDetails.manufacturer, firmwareDetails.model, firmwareDetails.version)) {
          for {
            firmware <- downloadFirmware(firmwareDetails)
            persistedFirmware <- persistFirmware(firmware)
          } yield persistedFirmware
        }
      } yield firmware
    }

    private def downloadSpecificVersion(manufacturer: Manufacturer, model: Model, version: Version)(using manufacturerHandler: DeviceManufacturerHandler) = {
      downloadIfNecessary(FirmwareKey(manufacturer, model, version)) {
        for {
          firmwareDetails <- manufacturerHandler.getFirmwareDownloadDetails(manufacturer, model, Some(version))
          firmware <- downloadFirmware(firmwareDetails)
          persistedFirmware <- persistFirmware(firmware)
        } yield persistedFirmware
      }
    }

    private def downloadIfNecessary(firmwareKey: FirmwareKey)(download: => IO[EsperError, Firmware]) = for {
      maybeFirmware <- firmwareRepository.getOpt(firmwareKey)
      firmware <- maybeFirmware match {
        case Some(firmware) => ZIO
          .logInfo(s"Skipping download - firmware already exists: $firmware")
          .as(firmware)
        case None => for {
          _ <- ZIO.logInfo (s"Downloading version ${firmwareKey.version} for ${firmwareKey.manufacturer} ${firmwareKey.model}")
          firmware <- download
        } yield firmware
      }
    } yield firmware

    private def downloadFirmware(firmwareDescriptor: FirmwareDescriptor): IO[FirmwareDownloadError, Firmware] = for {
      bytes <- httpClient.download(firmwareDescriptor.url.toString)
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

    private def persistFirmware(firmware: Firmware) = for {
      firmware <- firmwareRepository
        .add(firmware)
        .logError("Failed to persist firmware")
      _ <- pendingUpdateService.firmwareDownloaded(firmware)
    } yield firmware
  }

  val layer: URLayer[PendingUpdateService & HttpClient & FirmwareRepository & ManufacturerRepository, FirmwareDownloader] =
    ZLayer.fromFunction(FirmwareDownloaderLive(_, _, _, _))
}