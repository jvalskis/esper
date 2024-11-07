package is.valsk.esper.event

import zio.{Queue, Task, URLayer, ZIO, ZLayer}

trait FirmwareEventDispatcher extends EventDispatcher[FirmwareEvent, FirmwareEventListener]

object FirmwareEventDispatcher {

  private class FirmwareEventDispatcherLive(
      val eventQueue: Queue[FirmwareEvent],
      val listeners: List[FirmwareEventListener],
  ) extends FirmwareEventDispatcher {
    override def invokeListener(event: FirmwareEvent)(listener: FirmwareEventListener): Task[Unit] = listener.onFirmwareEvent(event)
  }

  val layer: URLayer[Queue[FirmwareEvent] & List[FirmwareEventListener], FirmwareEventDispatcher] = ZLayer.derive[FirmwareEventDispatcherLive]
}
