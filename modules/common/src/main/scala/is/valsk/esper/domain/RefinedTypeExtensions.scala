package is.valsk.esper.domain

import eu.timepit.refined.api.Refined

object RefinedTypeExtensions {

  given refinedToString[T, P]: Conversion[Refined[T, P], String] with
    def apply(value: Refined[T, P]): String = value.toString
    
  given stringToRefined[T, P]: Conversion[String, Refined[T, P]] with
    def apply(value: String): Refined[T, P] = value.asInstanceOf[Refined[T, P]]
}
