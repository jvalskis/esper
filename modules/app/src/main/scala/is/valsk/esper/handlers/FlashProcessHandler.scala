package is.valsk.esper.handlers

import com.raquo.airstream.ownership.OneTimeOwner
import com.raquo.laminar.api.L.{*, given}
import is.valsk.esper.core.BackendClient
import is.valsk.esper.core.ZJS.*
import is.valsk.esper.domain.DeviceStatus.UpdateStatus
import is.valsk.esper.domain.RefinedTypeExtensions.refinedToString
import is.valsk.esper.domain.{FlashResult, PendingUpdate}
import is.valsk.esper.handlers.FlashProcessHandler.*
import zio.ZIO

import scala.language.implicitConversions

class FlashProcessHandler(pendingUpdate: PendingUpdate) {
  val progress: Var[FlashProgress] = Var[FlashProgress](NotInProgress)
  progress.signal
    .map {
      case Start =>
        flashDevice
          .map {
            case FlashResult(_, currentVersion, UpdateStatus.done) => Done(Right(currentVersion.value))
            case _ => InProgress(50)
          }
          .tapError(e => {
            ZIO.attempt(progress.set(Done(Left(e.getMessage))))
          })
          .setTo(progress)
      case _ => ()
    }
    .observe(new OneTimeOwner(() => ()))

  private def flashDevice = {
    useBackend(_.ota.flashDeviceEndpoint(pendingUpdate.device.id, pendingUpdate.version.value))
  }
}

object FlashProcessHandler {

  trait Restartable {
    self: FlashProgress =>
    override def canStart: Boolean = true
  }

  trait FlashProgress {
    def canStart: Boolean = false

    def progressValue: Double = 0
  }

  case object NotInProgress extends FlashProgress with Restartable

  case object Start extends FlashProgress {
    override def progressValue: Double = 10d
  }

  case class InProgress(override val progressValue: Double) extends FlashProgress

  case class Done(result: Either[String, String]) extends FlashProgress with Restartable {
    override def progressValue: Double = 100d
  }
}