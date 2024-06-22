package is.valsk.esper.domain

import eu.timepit.refined.api.Refined
import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.domain.Types.UrlString

object RefinedTypeExtensions {

  given refinedToString[T, P]: Conversion[Refined[T, P], String] with
    def apply(value: Refined[T, P]): String = value.toString
    
  given stringToRefined[T, P]: Conversion[String, Refined[T, P]] with
    def apply(value: String): Refined[T, P] = value.asInstanceOf[Refined[T, P]]

  given stringToNonEmptyString[T <: NonEmptyString]: Conversion[String, T] with
    def apply(value: String): T = NonEmptyString.unsafeFrom(value).asInstanceOf[T]

  given stringToUrlString: Conversion[String, UrlString] with
    def apply(value: String): UrlString = UrlString(value)
}
