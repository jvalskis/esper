package is.valsk.esper.hass

import is.valsk.esper.domain.Device
import is.valsk.esper.hass.messages.responses.HassResult
import zio.IO

trait HassToDomainMapper {

  def toDomain(hassDevice: HassResult): IO[String, Device]

}
