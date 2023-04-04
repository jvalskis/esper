package is.valsk.esper.types

import eu.timepit.refined.api.{Refined, RefinedTypeOps}
import eu.timepit.refined.string.Url
import zio.json.{JsonDecoder, JsonEncoder}

type UrlString = String Refined Url

object UrlString extends RefinedTypeOps[UrlString, String] {
  implicit val encoder: JsonEncoder[UrlString] = JsonEncoder[String].contramap(_.toString)
  implicit val decoder: JsonDecoder[UrlString] = JsonDecoder[String].mapOrFail(UrlString.from)
}