package is.valsk.esper.components

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom.HTMLElement

object Anchors {
  def renderNavLink(text: String, location: String, cssClass: String = ""): ReactiveHtmlElement[HTMLElement] = {
    a(
      href := location,
      cls := cssClass,
      text
    )
  }
}