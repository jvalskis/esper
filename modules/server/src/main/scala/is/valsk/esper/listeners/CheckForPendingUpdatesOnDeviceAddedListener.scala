package is.valsk.esper.listeners

import is.valsk.esper.domain.*
import is.valsk.esper.event.*
import is.valsk.esper.services.PendingUpdateService
import zio.{Task, URLayer, ZIO, ZLayer}

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
    ZLayer.derive[CheckForPendingUpdatesOnDeviceAddedListenerLive]
}