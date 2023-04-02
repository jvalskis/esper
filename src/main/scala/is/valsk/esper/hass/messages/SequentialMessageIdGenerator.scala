package is.valsk.esper.hass.messages

import zio.*

class SequentialMessageIdGenerator extends MessageIdGenerator {

  private val messageId = Ref.make[Int](1)

  def generate(): UIO[Int] = for {
    messageIdRef <- messageId
    id <- messageIdRef.getAndUpdate(_ + 1)
  } yield id
}

object SequentialMessageIdGenerator {

  val layer: ULayer[MessageIdGenerator] = ZLayer.succeed(SequentialMessageIdGenerator())
}
