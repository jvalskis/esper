package is.valsk.esper.domain

import eu.timepit.refined.api.RefinedTypeOps
import eu.timepit.refined.types.string.NonEmptyString
import zio.json.{JsonCodec, JsonDecoder, JsonEncoder}

object Types {

  type Manufacturer = NonEmptyString

  object Manufacturer extends RefinedTypeOps[Manufacturer, String] {
    def apply(value: String): Manufacturer = NonEmptyString.unsafeFrom(value)
  }

  type Model = NonEmptyString

  object Model extends RefinedTypeOps[Model, String] {
    def apply(value: String): Model = NonEmptyString.unsafeFrom(value)
  }

  type DeviceId = NonEmptyString

  object DeviceId extends RefinedTypeOps[DeviceId, String] {
    def apply(value: String): DeviceId = NonEmptyString.unsafeFrom(value)
  }

  object NonEmptyStringImplicits {
    given encoder: JsonEncoder[NonEmptyString] = JsonEncoder[String].contramap(_.toString)

    given decoder: JsonDecoder[NonEmptyString] = JsonDecoder[String].mapOrFail(NonEmptyString.from)

    given codec: JsonCodec[NonEmptyString] = JsonCodec(encoder, decoder)
  }

  type Name = NonEmptyString

  object Name extends RefinedTypeOps[NonEmptyString, String] {
    def apply(value: String): DeviceId = NonEmptyString.unsafeFrom(value)
  }

  type UrlString = NonEmptyString//String Refined Url TODO issue with scalajs and refined Url type

  object UrlString extends RefinedTypeOps[NonEmptyString, String] {
    given encoder: JsonEncoder[NonEmptyString] = JsonEncoder[String].contramap(_.toString)

    given decoder: JsonDecoder[NonEmptyString] = JsonDecoder[String].mapOrFail(NonEmptyString.from)

    given codec: JsonCodec[NonEmptyString] = JsonCodec(encoder, decoder)

    def apply(value: String): UrlString = NonEmptyString.unsafeFrom(value)

    override def from(value: String): Either[String, UrlString] = NonEmptyString.from(value)
  }
}
