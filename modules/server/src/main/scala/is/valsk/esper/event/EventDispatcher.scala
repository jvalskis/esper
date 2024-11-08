package is.valsk.esper.event

import zio.{Queue, Task, ZIO}

trait EventDispatcher[E, L] {

  def run(): Task[Unit] = {
    for {
      event <- eventQueue.take
      _ <- ZIO.foreach(listeners)(invokeListener(event))
    } yield ()
  }.forever.forkDaemon.unit

  def eventQueue: Queue[E]

  def listeners: List[L]

  def invokeListener(event: E)(listener: L): Task[Unit]

}