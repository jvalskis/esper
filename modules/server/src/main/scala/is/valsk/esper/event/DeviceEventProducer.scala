package is.valsk.esper.event

import zio.{Queue, URLayer, ZLayer}

trait DeviceEventProducer extends EventProducer[DeviceEvent]

object DeviceEventProducer {

  private class DeviceEventProducerLive(
      val eventQueue: Queue[DeviceEvent],
  ) extends DeviceEventProducer
  
  val layer: URLayer[Queue[DeviceEvent], DeviceEventProducer] = ZLayer.derive[DeviceEventProducerLive]
}
