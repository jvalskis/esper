package is.valsk.esper.event

import zio.{Queue, UIO}

trait EventProducer[E] {

  def produceEvent(event: E): UIO[Unit] = {
    eventQueue.offer(event).unit
  }

  protected def eventQueue: Queue[E]
}
