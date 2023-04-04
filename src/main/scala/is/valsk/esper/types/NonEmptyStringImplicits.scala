package is.valsk.esper.types

import eu.timepit.refined.api.{Refined, RefinedTypeOps}
import eu.timepit.refined.string.Url
import eu.timepit.refined.types.string.NonEmptyString
import zio.json.{JsonDecoder, JsonEncoder}

object NonEmptyStringImplicits {
  implicit val encoder: JsonEncoder[NonEmptyString] = JsonEncoder[String].contramap(_.toString)
  implicit val decoder: JsonDecoder[NonEmptyString] = JsonDecoder[String].mapOrFail(NonEmptyString.from)
}