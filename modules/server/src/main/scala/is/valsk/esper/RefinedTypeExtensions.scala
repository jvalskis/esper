package is.valsk.esper

import eu.timepit.refined.api.Refined

object RefinedTypeExtensions {

  given refinedToString[T, P]: Conversion[Refined[T, P], String] with
    def apply(value: Refined[T, P]): String = value.toString
}
