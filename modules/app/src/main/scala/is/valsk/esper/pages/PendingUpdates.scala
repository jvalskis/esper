package is.valsk.esper.pages

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import is.valsk.esper.components.Anchors
import is.valsk.esper.core.ZJS.*
import is.valsk.esper.domain.{Device, PendingUpdate}
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import is.valsk.esper.domain.RefinedTypeExtensions.refinedToString
import scala.language.implicitConversions

object PendingUpdates {

  private val pendingUpdateBus = EventBus[List[PendingUpdate]]()

  def fetchPendingUpdates(): Unit = {
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

  private def renderAction(pendingUpdate: PendingUpdate) =
    div(
      cls := "pending-update-card-btn-apply",
      a(
        href := "https://todo.com",
        target := "blank",
        button(
          `type` := "button",
          cls := "btn btn-danger update-action-btn",
          "Update"
        )
      )
    )

  private def renderPendingUpdate(pendingUpdate: PendingUpdate) =
    div(
      cls := "pending-update-cards",
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
      renderAction(pendingUpdate)
    )
}
