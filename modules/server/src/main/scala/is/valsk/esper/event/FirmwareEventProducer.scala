package is.valsk.esper.event

import zio.{Queue, URLayer, ZLayer}

trait FirmwareEventProducer extends EventProducer[FirmwareEvent]

object FirmwareEventProducer {

  private class FirmwareEventProducerLive(
      val eventQueue: Queue[FirmwareEvent],
  ) extends FirmwareEventProducer
  
  val layer: URLayer[Queue[FirmwareEvent], FirmwareEventProducer] = ZLayer.derive[FirmwareEventProducerLive]
}
