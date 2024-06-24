package is.valsk.esper.pages

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import org.scalajs.dom.HTMLElement

object NotFound {

  def apply(): ReactiveHtmlElement[HTMLElement] = {
    div("Page not found")
  }
}
