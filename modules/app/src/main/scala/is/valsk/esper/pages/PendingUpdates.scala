package is.valsk.esper.pages

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import is.valsk.esper.components.Anchors
import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model, Name, UrlString}
import org.scalajs.dom
import org.scalajs.dom.HTMLElement
import is.valsk.esper.domain.{Device, PendingUpdate, Version}

object PendingUpdates {

  val dummyPendingUpdates = List(
    PendingUpdate(
      device = Device(
        id = DeviceId("10001"),
        url = UrlString("http://localhost/iot/acme-door-sensor-3000-1"),
        manufacturer = Manufacturer("ACME"),
        model = Model("WS5000"),
        name = Name("Window Sensor 5000"),
        nameByUser = Some("Small window"),
        softwareVersion = Some(Version("version1"))
      ),
      version = Version("version2")
    ),
    PendingUpdate(
      device = Device(
        id = DeviceId("10002"),
        url = UrlString("http://localhost/iot/acme-door-sensor-3000-2"),
        manufacturer = Manufacturer("ACME"),
        model = Model("DS3000"),
        name = Name("Door Sensor 3000"),
        nameByUser = Some("Big door"),
        softwareVersion = Some(Version("version10"))
      ),
      version = Version("version12")
    ),
    PendingUpdate(
      device = Device(
        id = DeviceId("10003"),
        url = UrlString("http://localhost/iot/acme-door-sensor-3000-3"),
        manufacturer = Manufacturer("ACME"),
        model = Model("DS3000"),
        name = Name("Door Sensor 3000"),
        nameByUser = None,
        softwareVersion = None
      ),
      version = Version("version12")
    ),
  )

  def apply(): ReactiveHtmlElement[HTMLElement] = {
    sectionTag(
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
            renderPendingUpdates()
          )
        )
      )
    )
  }

  private def renderPendingUpdates() = {
    dummyPendingUpdates.map { pendingUpdate =>
      renderPendingUpdate(pendingUpdate)
    }
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
