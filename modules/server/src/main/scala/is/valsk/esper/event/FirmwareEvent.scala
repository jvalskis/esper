package is.valsk.esper.event

import is.valsk.esper.domain.Firmware
import zio.{Queue, Task, URLayer, ZLayer}

sealed trait FirmwareEvent

case class FirmwareDownloaded(firmware: Firmware) extends FirmwareEvent

object FirmwareEvent {
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

  trait FirmwareEventListener {

    def onFirmwareEvent(firmwareEvent: FirmwareEvent): Task[Unit]
  }

  trait FirmwareEventProducer extends EventProducer[FirmwareEvent]

  object FirmwareEventProducer {

    private class FirmwareEventProducerLive(
        val eventQueue: Queue[FirmwareEvent],
    ) extends FirmwareEventProducer

    val layer: URLayer[Queue[FirmwareEvent], FirmwareEventProducer] = ZLayer.derive[FirmwareEventProducerLive]
  }
}