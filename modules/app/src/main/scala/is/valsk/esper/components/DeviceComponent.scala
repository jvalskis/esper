package is.valsk.esper.components

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import is.valsk.esper.domain.Device
import is.valsk.esper.domain.RefinedTypeExtensions.refinedToString
import org.scalajs.dom
import org.scalajs.dom.{HTMLDivElement, HTMLElement}

import scala.language.implicitConversions

object DeviceComponent {

  def apply(device: Device, deviceSignal: Signal[Device])(
      renderActionSlot: => ReactiveHtmlElement[HTMLElement],
      renderOverviewSlot: => ReactiveHtmlElement[HTMLElement]
  ): ReactiveHtmlElement[HTMLElement] = {
    div(
      cls := "col",
      div(
        cls := "pending-update-card-img",
        renderManufacturerIcon(device)
      ),
      div(
        cls := "pending-update-card-contents",
        h5(
          Anchors.renderNavLink(
            s"${device.name} ${device.nameByUser.fold("")(name => s" / $name")}",
            device.url,
            "device-title-link"
          )
        ),
        renderOverview(device, renderOverviewSlot),
      ),
      renderActionSlot
    )
  }

  private def renderManufacturerIcon(device: Device) =
    img(
      cls := "img-fluid",
      src := "TODO manufacturer placeholder",
      alt := device.manufacturer
    )

  def renderDetail(icon: String, value: String): ReactiveHtmlElement[HTMLElement] =
    div(
      cls := "device-detail",
      i(cls := s"fa $icon device-detail-icon"),
      p(
        cls := "device-detail-value",
        value
      )
    )

  private def renderOverview(device: Device, renderOverviewSlot: => ReactiveHtmlElement[HTMLElement]) =
    div(
      cls := "device-summary",
      renderDetail("fa-microchip", s"${device.manufacturer} / ${device.model}"),
      renderDetail("fa-sd-card", s"${device.softwareVersion.map(_.value).getOrElse("N/A")}"),
      renderOverviewSlot
    )

}
