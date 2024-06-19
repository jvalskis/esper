package is.valsk.esper.services

import is.valsk.esper.domain.{EsperError, *}
import is.valsk.esper.model.api.PendingUpdate
import is.valsk.esper.repositories.{DeviceRepository, FirmwareRepository, ManufacturerRepository, PendingUpdateRepository}
import is.valsk.esper.services.FirmwareService.LatestFirmwareStatus
import is.valsk.esper.services.FirmwareService.LatestFirmwareStatus.LatestFirmwareStatus
import zio.{IO, URLayer, ZIO, ZLayer}

trait PendingUpdateService {

  def deviceAdded(device: Device): IO[EsperError, Unit]

  def deviceRemoved(device: Device): IO[EsperError, Unit]

  def deviceUpdated(device: Device): IO[EsperError, Unit]

  def firmwareDownloaded(firmware: Firmware): IO[EsperError, Unit]

}

object PendingUpdateService {
  private class PendingUpdateServiceLive(
      deviceRepository: DeviceRepository,
      pendingUpdateRepository: PendingUpdateRepository,
      manufacturerRepository: ManufacturerRepository,
      firmwareRepository: FirmwareRepository,
      emailService: EmailService,
  ) extends PendingUpdateService {

    def deviceAdded(device: Device): IO[EsperError, Unit] = for {
      _ <- ZIO.logInfo("Checking device firmware status after new device was added...")
      _ <- checkDeviceVersion(device)
    } yield ()

    def deviceRemoved(device: Device): IO[EsperError, Unit] = {
      ZIO.unit
    }

    def deviceUpdated(device: Device): IO[EsperError, Unit] = {
      ZIO.unit
    }

    def firmwareDownloaded(firmware: Firmware): IO[EsperError, Unit] = for {
      _ <- ZIO.logInfo("Checking device firmware status after new firmware was downloaded...")
      devices <- deviceRepository.getAll
        .map(_.filter(device => device.manufacturer == firmware.manufacturer && device.model == firmware.model))
      result <- ZIO.foreach(devices)(checkDeviceVersion)
      _ <- sendNotificationIfNeeded(result.flatten)
    } yield ()

    private def sendNotificationIfNeeded(pendingUpdates: List[PendingUpdate]): IO[FailedToSendEmail, Unit] = pendingUpdates match {
      case Nil => ZIO.unit
      case updates => for {
        _ <- ZIO.logInfo(s"Sending firmware update notifications for ${updates.size} devices...")
        _ <- sendEmail(updates)
      } yield ()
    }

    private def checkDeviceVersion(device: Device): IO[EsperError, Option[PendingUpdate]] = for {
      status <- latestFirmwareStatus(device)
      result <- status match {
        case LatestFirmwareStatus.Outdated(latestFirmwareVersion) => addPendingUpdate(device, latestFirmwareVersion).map(Some(_))
        case _ => ZIO.succeed(None)
      }
    } yield result

    private def addPendingUpdate(device: Device, latestFirmwareVersion: Version): IO[EntityNotFound | PersistenceException, PendingUpdate] = for {
      maybePendingUpdate <- pendingUpdateRepository.getOpt(device.id)
      pendingUpdate <- maybePendingUpdate match {
        case Some(pendingUpdate) =>
          pendingUpdateRepository.update(pendingUpdate.copy(version = latestFirmwareVersion))
        case None =>
          pendingUpdateRepository.add(PendingUpdate(device, latestFirmwareVersion))
      }
    } yield pendingUpdate

    private def latestFirmwareStatus(device: Device): IO[EsperError, LatestFirmwareStatus] = for {
      manufacturerHandler <- manufacturerRepository.get(device.manufacturer)
      maybeLatestFirmware <- firmwareRepository.getLatestFirmware(device.manufacturer, device.model)(using manufacturerHandler.versionOrdering)
      maybeCurrentVersion <- ZIO.succeed(device.softwareVersion)
      status = (maybeCurrentVersion, maybeLatestFirmware.map(_.version)) match {
        case (None, _) | (_, None) => LatestFirmwareStatus.Undefined
        case (Some(currentVersion), Some(latestFirmwareVersion)) =>
          given ordering: Ordering[Version] = manufacturerHandler.versionOrdering

          if (latestFirmwareVersion > currentVersion)
            LatestFirmwareStatus.Outdated(latestFirmwareVersion)
          else
            LatestFirmwareStatus.Latest
      }
    } yield status

    private def sendEmail(updates: List[PendingUpdate]): IO[FailedToSendEmail, Unit] = {
      emailService
        .sendEmail(
          subject = s"Esper: there are ${updates.size} pending updates",
          content =
            s"""
               |<p>The following devices have pending firmware updates:</p>
               |<ul>
               |${updates.map(update => s"<li>${update.device.manufacturer} ${update.device.model} (${update.device.id}): ${update.version}</li>").mkString("\n")}
               |</ul>
               |""".stripMargin
        )
        .mapError(e => FailedToSendEmail(Some(e)))
        .logError
    }
  }

  val layer: URLayer[EmailService & DeviceRepository & FirmwareRepository & PendingUpdateRepository & ManufacturerRepository, PendingUpdateService] =
    ZLayer.fromFunction(PendingUpdateServiceLive(_, _, _, _, _))
}