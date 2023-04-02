package is.valsk.esper.hass.messages

import zio.*

trait MessageIdGenerator {

  def generate(): UIO[Int]
}

object MessageIdGenerator {

  def generate(): RIO[MessageIdGenerator, Int] = ZIO.serviceWithZIO[MessageIdGenerator](_.generate())
}
