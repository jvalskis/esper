package is.valsk.esper

import com.raquo.laminar.api.L.{*, given}
import frontroute.LinkHandler
import is.valsk.esper.components.{Header, Router}
import org.scalajs.dom

object App {

  private val app = div(
    Header(),
    Router(),
  ).amend(LinkHandler.bind)

  def main(args: Array[String]): Unit = {
    val containerNode = dom.document.querySelector("#app")

    render(
      containerNode,
      app
    )
    ()
  }
}
