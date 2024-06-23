package is.valsk.esper.pages

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import is.valsk.esper.components.ProgressBar.ProgressStatus
import is.valsk.esper.components.{DeviceComponent, ProgressBar}
import is.valsk.esper.core.BackendClient
import is.valsk.esper.core.ZJS.*
import is.valsk.esper.domain.PendingUpdate
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.handlers.FlashProcessHandler
import is.valsk.esper.handlers.FlashProcessHandler.*
import org.scalajs.dom.HTMLElement

import scala.language.implicitConversions

object PendingUpdates {

  private val pendingUpdateBus = EventBus[List[PendingUpdate]]()

  private def fetchPendingUpdates(): Unit = {
    useBackend(_.devices.getPendingUpdatesEndpoint(())).emitTo(pendingUpdateBus)
  }

  def apply(): ReactiveHtmlElement[HTMLElement] = {
    sectionTag(
      onMountCallback(_ => fetchPendingUpdates()),
      cls := "section-1",
      div(
        cls := "container device-list-hero",
        h1(
          cls := "device-list-title",
          "Pending Updates"
        )
      ),
      div(
        cls := "container",
        div(
          cls := "row pending-update-body",
          div(
            cls := "col-md",
            children <-- pendingUpdateBus.events.split(pendingUpdate => (pendingUpdate.device.id, pendingUpdate.version.value))(renderPendingUpdate)
          )
        )
      )
    )
  }

  private def renderPendingUpdate(id: (DeviceId, String), pendingUpdate: PendingUpdate, pendingUpdateSignal: Signal[PendingUpdate]) = {
    val flashProgress = new FlashProcessHandler(pendingUpdate).progress
    div(
      cls := "pending-update-cards",
      div(
        cls := "container",
        div(
          cls := "row",
          DeviceComponent(
            device = pendingUpdate.device,
            deviceSignal = pendingUpdateSignal.map(_.device),
          )(
            renderActionSlot = renderAction(pendingUpdate, flashProgress),
            renderOverviewSlot = DeviceComponent.renderDetail("fa-sd-card yellow", s"${pendingUpdate.version.value}"),
          ),
        ),
        renderProgress(flashProgress.signal)
      )
    )
  }

  private def renderAction(pendingUpdate: PendingUpdate, flashProgress: Var[FlashProgress]): ReactiveHtmlElement[HTMLElement] = {
    div(
      cls := "pending-update-card-btn-apply",
      button(
        `type` := "button",
        cls := "btn btn-danger update-action-btn",
        onClick.filter(_ => flashProgress.now().canStart) --> (_ => flashProgress.set(Start)),
        disabled <-- flashProgress.signal.map(_.canStart).map(!_),
        "Update"
      )
    )
  }

  private def renderProgress(flashProgress: Signal[FlashProgress]) = {
    val progressSignal = flashProgress.map {
      case result@Done(Right(_)) => ProgressStatus(result.progressValue, "bg-success")
      case result@Done(Left(_)) => ProgressStatus(result.progressValue, "bg-danger")
      case result => ProgressStatus(result.progressValue)
    }
    child.maybe <-- flashProgress.map {
      case NotInProgress => None
      case flashProgress => Some(
        div(
          cls := "row",
          div(
            cls := "pending-update-card-progress",
            div(
              cls := "pending-update-card-flash-progress",
              ProgressBar(progressSignal),
              renderProgressStatusInfo(flashProgress)
            )
          )
        )
      )
    }
  }

  private def renderProgressStatusInfo(flashProgress: FlashProgress) = {
    flashProgress match {
      case Start => "Starting..."
      case InProgress(_) => "Flashing..."
      case Done(result) => result.fold(x => s"Error: $x", _ => "Done!")
      case _ => "Unknown?"
    }
  }
}
