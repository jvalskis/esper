package is.valsk.esper.event

import zio.{Queue, Ref, Task, ZIO}

trait EventDispatcher[E, L] {

  def run(): Task[Unit] = {
    for {
      event <- eventQueue.take
      _ <- ZIO.collectAllPar(
        listeners.map(invokeListener(event))
      )
    } yield ()
  }.forever.forkDaemon.unit

  def eventQueue: Queue[E]

  def listeners: Seq[L]

  def invokeListener(event: E)(listener: L): Task[Unit]

}