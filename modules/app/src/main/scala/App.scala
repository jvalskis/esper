package is.valsk.esper

import com.raquo.laminar.api.L.{*, given}
import org.scalajs.dom

object App {

  def main(args: Array[String]): Unit = {
    val containerNode = dom.document.querySelector("#app")
    val app = div(
      h1("Hello, world!"),
      p("This is a simple example of Laminar."),
      p("It is a reactive web framework for Scala.js."),
    )

    render(
      containerNode,
      app
    )
    ()
  }
}
