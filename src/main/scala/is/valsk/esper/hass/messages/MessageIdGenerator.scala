package is.valsk.esper.hass.messages

import zio.*

trait MessageIdGenerator {

  def generate(): UIO[Int]
}