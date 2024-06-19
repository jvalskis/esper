package is.valsk.esper.hass

import is.valsk.esper.domain.{Device, MalformedVersion, Version}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import zio.IO

trait HassToDomainMapper {

  def toDomain(hassDevice: HassResult): IO[MalformedVersion | ParseError, Device]

  def parseVersion(version: String): Either[MalformedVersion, Version]
}
