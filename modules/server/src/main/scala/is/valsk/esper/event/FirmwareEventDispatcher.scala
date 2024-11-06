package is.valsk.esper.event

import zio.{Queue, Ref, Task, URLayer, ZIO, ZLayer}

object FirmwareEventDispatcher {

  private class FirmwareEventDispatcherLive(
      eventQueue: Queue[FirmwareEvent],
      listeners: Seq[FirmwareEventListener],
  ) extends EventDispatcher[FirmwareEvent, FirmwareEventListener] {
    override def invokeListener(event: FirmwareEvent)(listener: FirmwareEventListener): Task[Unit] = listener.onFirmwareEvent(event)
  }

  val layer: URLayer[Queue[FirmwareEvent] & Seq[FirmwareEventListener], EventDispatcher[FirmwareEvent, FirmwareEventListener]] = ZLayer.fromFunction(FirmwareEventDispatcherLive(_, _))
}
