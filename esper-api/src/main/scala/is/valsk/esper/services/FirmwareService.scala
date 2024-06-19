package is.valsk.esper.services

import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import is.valsk.esper.services.FirmwareService.LatestFirmwareStatus
import zio.{IO, URLayer, ZIO, ZLayer}

trait FirmwareService {

  def getFirmware(manufacturer: Manufacturer, model: Model, version: Version): IO[EsperError, Firmware]

  def getLatestFirmware(manufacturer: Manufacturer, model: Model): IO[EsperError, Firmware]

  def getOrDownloadFirmware(manufacturer: Manufacturer, model: Model, version: Version): IO[EsperError, Firmware]

  def getOrDownloadLatestFirmware(manufacturer: Manufacturer, model: Model): IO[EsperError, Firmware]

}

object FirmwareService {
  private class FirmwareServiceLive(
      firmwareRepository: FirmwareRepository,
      manufacturerRepository: ManufacturerRepository,
      firmwareDownloader: FirmwareDownloader,
      pendingUpdateService: PendingUpdateService,
  ) extends FirmwareService {

    override def getFirmware(manufacturer: Manufacturer, model: Model, version: Version): IO[EsperError, Firmware] = {
      firmwareRepository
        .get(FirmwareKey(manufacturer, model, version))
        .mapError {
          case EntityNotFound(_) => FirmwareNotFound("Firmware not found", manufacturer, model, Some(version))
          case x => x
        }
    }

    override def getLatestFirmware(manufacturer: Manufacturer, model: Model): IO[EsperError, Firmware] = for {
      manufacturerHandler <- manufacturerRepository.get(manufacturer).catchSome {
        case EntityNotFound(_) => ZIO.fail(ManufacturerNotSupported(manufacturer))
      }
      latestFirmware <- firmwareRepository.getLatestFirmware(manufacturer, model)(using manufacturerHandler.versionOrdering)
        .flatMap {
          case None => ZIO.fail(FirmwareNotFound("Latest firmware not found", manufacturer, model, None))
          case Some(result) => ZIO.succeed(result)
        }
    } yield latestFirmware

    override def getOrDownloadFirmware(manufacturer: Manufacturer, model: Model, version: Version): IO[EsperError, Firmware] = for {
      manufacturerHandler <- manufacturerRepository.get(manufacturer)
      firmware <- downloadIfNecessary(FirmwareKey(manufacturer, model, version)) {
        for {
          _ <- ZIO.logInfo(s"Downloading firmware version=$version for $manufacturer $model")
          firmware <- firmwareDownloader.downloadFirmware(manufacturer, model, version)(using manufacturerHandler)
          persistedFirmware <- persistFirmware(firmware)
        } yield persistedFirmware
      }
    } yield firmware

    override def getOrDownloadLatestFirmware(manufacturer: Manufacturer, model: Model): IO[EsperError, Firmware] = for {
      manufacturerHandler <- manufacturerRepository.get(manufacturer).catchSome {
        case EntityNotFound(_) => ZIO.fail(ManufacturerNotSupported(manufacturer))
      }
      firmwareDetails <- manufacturerHandler.getFirmwareDownloadDetails(manufacturer, model, None)
      firmware <- downloadIfNecessary(firmwareDetails.toFirmwareKey) {
        for {
          _ <- ZIO.logInfo(s"Downloading latest firmware version for $manufacturer $model")
          firmware <- firmwareDownloader.downloadFirmware(firmwareDetails)
          persistedFirmware <- persistFirmware(firmware)
        } yield persistedFirmware
      }
    } yield firmware

    private def downloadIfNecessary(firmwareKey: FirmwareKey)(download: => IO[EsperError, Firmware]) = for {
      maybeFirmware <- firmwareRepository.getOpt(firmwareKey)
      firmware <- maybeFirmware match {
        case Some(firmware) => ZIO
          .logInfo(s"Skipping download - firmware already exists: $firmware")
          .as(firmware)
        case None => for {
          _ <- ZIO.logInfo(s"Downloading version ${firmwareKey.version} for ${firmwareKey.manufacturer} ${firmwareKey.model}")
          firmware <- download
        } yield firmware
      }
    } yield firmware

    private def persistFirmware(firmware: Firmware) = for {
      firmware <- firmwareRepository
        .add(firmware)
        .logError("Failed to persist firmware")
      _ <- pendingUpdateService.firmwareDownloaded(firmware)
    } yield firmware
  }

  val layer: URLayer[FirmwareRepository & ManufacturerRepository & FirmwareDownloader & PendingUpdateService, FirmwareService] = ZLayer.fromFunction(FirmwareServiceLive(_, _, _, _))

  object LatestFirmwareStatus {
    sealed trait LatestFirmwareStatus

    case object Latest extends LatestFirmwareStatus

    case class Outdated(newVersion: Version) extends LatestFirmwareStatus

    case object Undefined extends LatestFirmwareStatus
  }
}
