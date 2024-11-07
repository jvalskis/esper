package is.valsk.esper.listeners

import is.valsk.esper.domain.*
import is.valsk.esper.event.*
import is.valsk.esper.repositories.DeviceRepository
import is.valsk.esper.services.{EmailService, PendingUpdateService}
import zio.{Task, URLayer, ZIO, ZLayer}

object CheckForPendingUpdatesOnFirmwareAddedListener {
  class CheckForPendingUpdatesOnFirmwareAddedListenerLive(
      pendingUpdateService: PendingUpdateService,
      deviceRepository: DeviceRepository,
      emailService: EmailService,
  ) extends FirmwareEventListener {

    override def onFirmwareEvent(event: FirmwareEvent): Task[Unit] = event match {
      case FirmwareDownloaded(firmware) => {
        for {
          _ <- ZIO.logInfo("Checking device firmware status after new firmware was downloaded...")
          devices <- deviceRepository.getAll
            .map(_.filter(device => device.manufacturer == firmware.manufacturer && device.model == firmware.model))
          result <- ZIO.foreach(devices)(pendingUpdateService.checkForPendingUpdates)
          _ <- sendNotificationIfNeeded(result.flatten)
        } yield ()
      }.catchAll {
        case EmailDeliveryError(message, _) =>
          ZIO.logError(s"Failed to send email: $message")
      }
    }

    private def sendNotificationIfNeeded(pendingUpdates: List[PendingUpdate]): Task[Unit] = pendingUpdates match {
      case Nil => ZIO.unit
      case updates => for {
        _ <- ZIO.logInfo(s"Sending firmware update notifications for ${updates.size} devices...")
        _ <- sendEmail(updates).retryN(3)
      } yield ()
    }

    private def sendEmail(updates: List[PendingUpdate]): Task[Unit] = {
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
    }
  }

  val layer: URLayer[EmailService & DeviceRepository & PendingUpdateService, CheckForPendingUpdatesOnFirmwareAddedListenerLive] =
    ZLayer.fromFunction(CheckForPendingUpdatesOnFirmwareAddedListener.CheckForPendingUpdatesOnFirmwareAddedListenerLive(_, _, _))
}