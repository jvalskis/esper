package is.valsk.esper.hass.messages

import is.valsk.esper.hass.messages.MessageParser.ParseError
import zio.json.{DeriveJsonDecoder, DeriveJsonEncoder, JsonDecoder, JsonEncoder}

enum Type(val typeName: String) {
  case Auth extends Type("auth")
  case DeviceRegistryList extends Type("config/device_registry/list")

  case AuthRequired extends Type("auth_required")
  case AuthOK extends Type("auth_ok")
  case AuthInvalid extends Type("auth_invalid")
  case Result extends Type("result")
}

object Type {
  implicit val typeDecoder: JsonDecoder[Type] = DeriveJsonDecoder.gen[Type]
  implicit val typeEncoder: JsonEncoder[Type] = DeriveJsonEncoder.gen[Type]

  def parse(typeName: String): Either[ParseError, Type] = Type.values.find(_.typeName == typeName).toRight(ParseError(s"Unknown type: $typeName"))
}