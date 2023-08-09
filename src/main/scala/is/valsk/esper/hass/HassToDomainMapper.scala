package is.valsk.esper.hass

import is.valsk.esper.domain.{Device, MalformedVersion, Version}
import is.valsk.esper.hass.messages.responses.HassResult
import zio.{IO, UIO}

trait HassToDomainMapper {

  def toDomain(hassDevice: HassResult): IO[String, Device]

  def parseVersion(version: String): Either[MalformedVersion, Version]
}
