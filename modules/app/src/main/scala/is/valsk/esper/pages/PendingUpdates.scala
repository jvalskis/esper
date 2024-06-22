package is.valsk.esper.pages

import com.raquo.airstream.ownership.OneTimeOwner
import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import is.valsk.esper.components.Anchors
import is.valsk.esper.core.BackendClient
import is.valsk.esper.core.ZJS.*
import is.valsk.esper.domain.DeviceStatus.UpdateStatus
import is.valsk.esper.domain.{Device, FlashResult, PendingUpdate}
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import is.valsk.esper.domain.RefinedTypeExtensions.refinedToString
import zio.ZIO

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
            children <-- pendingUpdateBus.events.map(_.map(renderPendingUpdate))
          )
        )
      )
    )
  }

  private def renderManufacturerIcon(device: Device) =
    img(
      cls := "img-fluid",
      src := "TODO manufacturer placeholder",
      alt := device.manufacturer
    )

  private def renderDetail(icon: String, value: String) =
    div(
      cls := "device-detail",
      i(cls := s"fa $icon device-detail-icon"),
      p(
        cls := "device-detail-value",
        value
      )
    )

  private def renderOverview(update: PendingUpdate) =
    div(
      cls := "device-summary",
      renderDetail("fa-microchip", s"${update.device.manufacturer} / ${update.device.model}"),
      renderDetail("fa-sd-card", s"${update.device.softwareVersion.map(_.value).getOrElse("N/A")}"),
      renderDetail("fa-sd-card yellow", s"${update.version.value}"),
    )

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

  private def renderAction(pendingUpdate: PendingUpdate, flashProgress: Var[FlashProgress]) = {
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

  private def renderPendingUpdate(pendingUpdate: PendingUpdate) = {
    val flashProgress = Var[FlashProgress](NotInProgress)
    flashProgress.signal.map(println).observe(new OneTimeOwner(() => ()))
    flashProgress.signal.map {
      case Start =>
        useBackend(_.ota.flashDeviceEndpoint(pendingUpdate.device.id, pendingUpdate.version.value))
          .map {
            case FlashResult(_, currentVersion, UpdateStatus.done) => Done(Right(currentVersion.value))
            case _ => InProgress(50)
          }
          .tapError(e => {
            ZIO.attempt(flashProgress.set(Done(Left(e.getMessage))))
          })
          .setTo(flashProgress)
      case _ => ()
    }.observe(new OneTimeOwner(() => ()))
    div(
      cls := "pending-update-cards",
      div(
        cls := "container",
        div(
          cls := "row",
          div(
            cls := "col",
            div(
              cls := "pending-update-card-img",
              renderManufacturerIcon(pendingUpdate.device)
            ),
            div(
              cls := "pending-update-card-contents",
              h5(
                Anchors.renderNavLink(
                  s"${pendingUpdate.device.name} ${pendingUpdate.device.nameByUser.fold("")(name => s" / $name")}",
                  pendingUpdate.device.url,
                  "device-title-link"
                )
              ),
              renderOverview(pendingUpdate),
            ),
            renderAction(pendingUpdate, flashProgress),
          )
        ),
        child.maybe <-- flashProgress.signal.map {
          case NotInProgress => None
          case x => Some(
            div(
              cls := "row",
              div(
                cls := "pending-update-card-progress",
                div(
                  cls := "pending-update-card-flash-progress",
                  renderProgressBar(flashProgress.signal),
                  x match {
                    case Start => "Starting..."
                    case InProgress(_) => "Flashing..."
                    case Done(result) => result.fold(x => s"Error: $x", _ => "Done!")
                    case _ => "Unknown?"
                  }
                )
              )
            )
          )
        }
      )
    )
  }

  private def renderProgressBar(signal: StrictSignal[FlashProgress]) = {
    val percentSignal = signal.map {
      case Done(_) => 100d
      case _ => 75d
    }
    div(
      cls := "progress",
      role := "progressbar",
      aria.label := "Firmware flash process progress",
      aria.valueNow <-- percentSignal,
      aria.valueMin := 0,
      aria.valueMax := 100,
      div(
        cls <-- signal
          .map {
            case Done(Left(_)) => "bg-danger"
            case _ => ""
          }
          .map(value => s"$value progress-bar progress-bar-striped progress-bar-animated"),
        width <-- percentSignal.map(value => s"$value%")
      )
    )
  }
}
