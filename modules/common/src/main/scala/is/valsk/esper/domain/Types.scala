package is.valsk.esper.domain

//import eu.timepit.refined.api.{Refined, RefinedTypeOps}
//import eu.timepit.refined.string.Url
//import eu.timepit.refined.types.string.NonEmptyString
//import zio.json.{DeriveJsonDecoder, JsonCodec, JsonDecoder, JsonEncoder}

object Types {

  type Manufacturer = String//NonEmptyString

  object Manufacturer /*extends RefinedTypeOps[Manufacturer, String] */{
//    def apply(value: String): Manufacturer = NonEmptyString.unsafeFrom(value)
    def apply(value: String): String = value
    def unapply(value: String): Option[String] = Some(value)
    def from(value: String): Either[String, String] = Right(value)
  }

  type Model = String

  object Model /*extends RefinedTypeOps[Model, String]*/ {
//    def apply(value: String): Model = NonEmptyString.unsafeFrom(value)
    def apply(value: String): String = value
    def unapply(value: String): Option[String] = Some(value)
    def from(value: String): Either[String, String] = Right(value)
  }

  type DeviceId = String//NonEmptyString

  object DeviceId /*extends RefinedTypeOps[DeviceId, String]*/ {
//    def apply(value: String): DeviceId = NonEmptyString.unsafeFrom(value)
    def apply(value: String): String = value
    def unapply(value: String): Option[String] = Some(value)
    def from(value: String): Either[String, String] = Right(value)
  }

  object NonEmptyStringImplicits {
//    given encoder: JsonEncoder[NonEmptyString] = JsonEncoder[String].contramap(_.toString)
//
//    given decoder: JsonDecoder[NonEmptyString] = JsonDecoder[String].mapOrFail(NonEmptyString.from)
//
//    given codec: JsonCodec[NonEmptyString] = JsonCodec(encoder, decoder)
  }

  type Name = String//NonEmptyString

  object Name/* extends RefinedTypeOps[NonEmptyString, String] */{
//    def apply(value: String): DeviceId = NonEmptyString.unsafeFrom(value)
    def apply(value: String): String = value
    def unapply(value: String): Option[String] = Some(value)
  }

  type UrlString = String //Refined Url

  object UrlString /*extends RefinedTypeOps[UrlString, String]*/ {
//    given encoder: JsonEncoder[UrlString] = JsonEncoder[String].contramap(_.toString)
//
//    given decoder: JsonDecoder[UrlString] = JsonDecoder[String].mapOrFail(UrlString.from)
//
//    given codec: JsonCodec[UrlString] = JsonCodec(encoder, decoder)
//
//    def apply(value: String): UrlString = UrlString.unsafeFrom(value)
//    given encoder: JsonEncoder[UrlString] = JsonEncoder[UrlString].contramap(identity)
//
//    given decoder: JsonDecoder[UrlString] = JsonDecoder[UrlString].map(identity)
//
//    given codec: JsonCodec[UrlString] = JsonCodec(encoder, decoder)
    def apply(value: String): String = value
    def unapply(value: String): Option[String] = Some(value)
    def from(value: String): Either[String, String] = Right(value)
  }
}
