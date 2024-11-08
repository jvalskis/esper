package is.valsk.esper.services

import is.valsk.esper.domain.*
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository, PendingUpdateRepository}
import is.valsk.esper.services.FirmwareService.LatestFirmwareStatus
import is.valsk.esper.services.FirmwareService.LatestFirmwareStatus.LatestFirmwareStatus
import zio.{IO, URLayer, ZIO, ZLayer}

trait PendingUpdateService {
  def checkForPendingUpdates(device: Device): IO[EsperError, Option[PendingUpdate]]
}

object PendingUpdateService {
  private class PendingUpdateServiceLive(
      pendingUpdateRepository: PendingUpdateRepository,
      manufacturerRepository: ManufacturerRepository,
      firmwareRepository: FirmwareRepository,
  ) extends PendingUpdateService {

    override def checkForPendingUpdates(device: Device): IO[EsperError, Option[PendingUpdate]] = for {
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
      manufacturerHandler <- manufacturerRepository.get(device.manufacturer).catchSome {
        case EntityNotFound(_) => ZIO.fail(ManufacturerNotSupported(device.manufacturer))
      }
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
  }

  val layer: URLayer[FirmwareRepository & PendingUpdateRepository & ManufacturerRepository, PendingUpdateService] =
    ZLayer.fromFunction(PendingUpdateServiceLive(_, _, _))
}