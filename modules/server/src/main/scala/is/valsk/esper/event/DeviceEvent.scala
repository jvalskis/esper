package is.valsk.esper.event

import is.valsk.esper.domain.Device
import zio.{Queue, Task, URLayer, ZLayer}

sealed trait DeviceEvent

case class DeviceAdded(device: Device) extends DeviceEvent

case class DeviceUpdated(device: Device) extends DeviceEvent

case class DeviceRemoved(device: Device) extends DeviceEvent

object DeviceEvent {
  trait DeviceEventDispatcher extends EventDispatcher[DeviceEvent, DeviceEventListener]

  object DeviceEventDispatcher {

    private class DeviceEventDispatcherLive(
        val eventQueue: Queue[DeviceEvent],
        val listeners: List[DeviceEventListener],
    ) extends DeviceEventDispatcher {
      override def invokeListener(event: DeviceEvent)(listener: DeviceEventListener): Task[Unit] = listener.onDeviceEvent(event)
    }

    val layer: URLayer[List[DeviceEventListener], DeviceEventDispatcher] = ZLayer.derive[DeviceEventDispatcherLive]
  }

  trait DeviceEventListener {

    def onDeviceEvent(deviceEvent: DeviceEvent): Task[Unit]
  }

  trait DeviceEventProducer extends EventProducer[DeviceEvent]

  object DeviceEventProducer {

    private class DeviceEventProducerLive(
        val eventQueue: Queue[DeviceEvent],
    ) extends DeviceEventProducer

    val layer: URLayer[Queue[DeviceEvent], DeviceEventProducer] = ZLayer.derive[DeviceEventProducerLive]
  }

}