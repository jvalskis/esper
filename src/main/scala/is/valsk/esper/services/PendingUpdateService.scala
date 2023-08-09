package is.valsk.esper.services

import is.valsk.esper.EsperConfig
import is.valsk.esper.device.DeviceManufacturerHandler
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
import is.valsk.esper.hass.HassToDomainMapper
import is.valsk.esper.model.api.PendingUpdate
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.{DeviceRepository, FirmwareRepository, ManufacturerRepository, PendingUpdateRepository}
import is.valsk.esper.services.FirmwareService.LatestFirmwareStatus
import zio.nio.file.{Files, Path}
import zio.stream.{ZChannel, ZSink, ZStream}
import zio.{IO, ULayer, URLayer, ZIO, ZLayer}

trait PendingUpdateService {

  def deviceAdded(device: Device): IO[EsperError, Unit]

  def deviceRemoved(device: Device): IO[EsperError, Unit]

  def deviceUpdated(device: Device): IO[EsperError, Unit]

  def firmwareDownloaded(firmware: Firmware): IO[EsperError, Unit]

}

object PendingUpdateService {
  private class PendingUpdateServiceLive(
      deviceRepository: DeviceRepository,
      firmwareService: FirmwareService,
      pendingUpdateRepository: PendingUpdateRepository
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
      _ <- ZIO.foreach(devices)(checkDeviceVersion)
    } yield ()

    private def checkDeviceVersion(device: Device) = {
      for {
        status <- firmwareService.latestFirmwareStatus(device)
        _ <- status match {
          case LatestFirmwareStatus.Outdated(latestFirmwareVersion) => addPendingUpdate(device, latestFirmwareVersion)
          case _ => ZIO.unit
        }
      } yield ()
    }

    private def addPendingUpdate(device: Device, latestFirmwareVersion: Version) = {
      for {
        maybePendingUpdate <- pendingUpdateRepository.getOpt(device.id)
        _ <- maybePendingUpdate match {
          case Some(pendingUpdate) =>
            pendingUpdateRepository.update(pendingUpdate.copy(version = latestFirmwareVersion))
          case None =>
            pendingUpdateRepository.add(PendingUpdate(device, latestFirmwareVersion))
        }
      } yield ()
    }
  }

  val layer: URLayer[DeviceRepository & FirmwareService & PendingUpdateRepository, PendingUpdateService] = ZLayer.fromFunction(PendingUpdateServiceLive(_, _, _))
}