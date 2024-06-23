package is.valsk.esper.components

import com.raquo.laminar.api.L.{*, given}
import com.raquo.laminar.nodes.ReactiveHtmlElement
import is.valsk.esper.core.ZJS.*
import org.scalajs.dom
import org.scalajs.dom.HTMLElement

object ProgressBar {

  def apply(signal: Signal[ProgressStatus]): ReactiveHtmlElement[HTMLElement] = {
    div(
      cls := "progress",
      role := "progressbar",
      aria.label := "Firmware flash process progress",
      aria.valueNow <-- signal.map(_.percent),
      aria.valueMin := 0,
      aria.valueMax := 100,
      div(
        cls := "progress-bar progress-bar-striped progress-bar-animated",
        cls <-- signal.map(value => s"${value.cls}"),
        width <-- signal.map(value => s"${value.percent}%")
      )
    )
  }

  case class ProgressStatus(percent: Double, cls: String = "")
}