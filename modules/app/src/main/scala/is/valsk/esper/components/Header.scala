package is.valsk.esper.components

import com.raquo.laminar.api.L.*
import com.raquo.laminar.codecs.StringAsIsCodec
import com.raquo.laminar.nodes.ReactiveHtmlElement
import is.valsk.esper.common.Constants
import org.scalajs.dom
import org.scalajs.dom.HTMLElement

object  Header {
  def apply(): ReactiveHtmlElement[HTMLElement] = {
    div(
      cls := "container-fluid p-0",
      div(
        cls := "esper-nav-bar",
        div(
          cls := "container",
          navTag(
            cls := "navbar navbar-expand-lg navbar-light esper-nav",
            div(
              cls := "container",
              renderLogo(),
              button(
                cls := "navbar-toggler",
                `type` := "button",
                htmlAttr("data-bs-toggle", StringAsIsCodec) := "collapse",
                htmlAttr("data-bs-target", StringAsIsCodec) := "#navbarNav",
                htmlAttr("aria-controls", StringAsIsCodec) := "navbarNav",
                htmlAttr("aria-expanded", StringAsIsCodec) := "false",
                htmlAttr("aria-label", StringAsIsCodec) := "Toggle navigation",
                span(cls := "navbar-toggler-icon")
              ),
              div(
                cls := "collapse navbar-collapse",
                idAttr := "navbarNav2",
                ul(
                  cls := "navbar-nav ms-auto menu align-center expanded text-center",
                  renderNavLinks()
                )
              )
            )
          )
        )
      )
    )
  }

  private def renderLogo() = a(
    href := "/",
    cls := "navbar-brand",
    img(
      src := Constants.LogoImage,
      cls := "home-logo",
      alt := "Esper",
    )
  )

  private def renderNavLinks() = List(
    renderNavLink("Pending Updates", "pending-updates"),
  )

  private def renderNavLink(text: String, location: String) = {
    li(
      cls := "nav-item",
      Anchors.renderNavLink(text, location, "nav-link esper-item")
    )
  }
}