package is.valsk.esper.services

import is.valsk.esper.domain.*
import is.valsk.esper.domain.PendingUpdate
import is.valsk.esper.event.{DeviceAdded, DeviceEvent, DeviceEventListener, FirmwareDownloaded, FirmwareEvent, FirmwareEventListener}
import is.valsk.esper.repositories.{DeviceRepository, FirmwareRepository, ManufacturerRepository, PendingUpdateRepository}
import is.valsk.esper.services.FirmwareService.LatestFirmwareStatus
import is.valsk.esper.services.FirmwareService.LatestFirmwareStatus.LatestFirmwareStatus
import zio.{IO, Queue, Task, UIO, URLayer, ZIO, ZLayer}

object CheckForPendingUpdatesOnDeviceAddedListener {
  class CheckForPendingUpdatesOnDeviceAddedListenerLive(
      pendingUpdateService: PendingUpdateService,
  ) extends DeviceEventListener {

    override def onDeviceEvent(event: DeviceEvent): Task[Unit] = event match {
      case DeviceAdded(device) => for {
        _ <- ZIO.logInfo("Checking device firmware status after new device was added...")
        _ <- pendingUpdateService.checkForPendingUpdates(device)
      } yield ()
      case _ => ZIO.unit
    }
  }

  val layer: URLayer[PendingUpdateService, CheckForPendingUpdatesOnDeviceAddedListenerLive] =
    ZLayer.fromFunction(CheckForPendingUpdatesOnDeviceAddedListenerLive(_))
}