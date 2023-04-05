package is.valsk.esper.domain

import eu.timepit.refined.api.{Refined, RefinedTypeOps}
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString
import zio.json.{JsonDecoder, JsonEncoder}

object Types {

  type Manufacturer = NonEmptyString
  val Manufacturer = NonEmptyString

  type Model = NonEmptyString
  val Model = NonEmptyString

  object NonEmptyStringImplicits {
    implicit val encoder: JsonEncoder[NonEmptyString] = JsonEncoder[String].contramap(_.toString)
    implicit val decoder: JsonDecoder[NonEmptyString] = JsonDecoder[String].mapOrFail(NonEmptyString.from)
  }

  type UrlString = String Refined Url

  object UrlString extends RefinedTypeOps[UrlString, String] {
    implicit val encoder: JsonEncoder[UrlString] = JsonEncoder[String].contramap(_.toString)
    implicit val decoder: JsonDecoder[UrlString] = JsonDecoder[String].mapOrFail(UrlString.from)
  }
}