package is.valsk.esper.event

import is.valsk.esper.domain.PendingUpdate
import zio.{Queue, Task, URLayer, ZLayer}

sealed trait PendingUpdateEvent

case class PendingUpdateAdded(PendingUpdate: PendingUpdate) extends PendingUpdateEvent

object PendingUpdateEvent {
  trait PendingUpdateEventDispatcher extends EventDispatcher[PendingUpdateEvent, PendingUpdateEventListener]

  object PendingUpdateEventDispatcher {

    private class PendingUpdateEventDispatcherLive(
        val eventQueue: Queue[PendingUpdateEvent],
        val listeners: List[PendingUpdateEventListener],
    ) extends PendingUpdateEventDispatcher {
      override def invokeListener(event: PendingUpdateEvent)(listener: PendingUpdateEventListener): Task[Unit] = listener.onPendingUpdateEvent(event)
    }

    val layer: URLayer[Queue[PendingUpdateEvent] & List[PendingUpdateEventListener], PendingUpdateEventDispatcher] = ZLayer.derive[PendingUpdateEventDispatcherLive]
  }

  trait PendingUpdateEventListener {

    def onPendingUpdateEvent(PendingUpdateEvent: PendingUpdateEvent): Task[Unit]
  }

  trait PendingUpdateEventProducer extends EventProducer[PendingUpdateEvent]

  object PendingUpdateEventProducer {

    private class PendingUpdateEventProducerLive(
        val eventQueue: Queue[PendingUpdateEvent],
    ) extends PendingUpdateEventProducer

    val layer: URLayer[Queue[PendingUpdateEvent], PendingUpdateEventProducer] = ZLayer.derive[PendingUpdateEventProducerLive]
  }
}