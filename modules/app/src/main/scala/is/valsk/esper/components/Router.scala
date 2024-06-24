package is.valsk.esper.components

import com.raquo.laminar.api.L.*
import com.raquo.laminar.nodes.ReactiveHtmlElement
import org.scalajs.dom
import frontroute.*
import is.valsk.esper.pages.{NotFound, PendingUpdates}
import org.scalajs.dom.HTMLElement

object Router {
  def apply(): ReactiveHtmlElement[HTMLElement] = {
    mainTag(
      routes(
        div(
          cls := "container-fluid p-0",
          (pathEnd | path("pending-updates")) {
            PendingUpdates()
          },
          noneMatched(
            NotFound()
          )
        )
      )
    )
  }

}
