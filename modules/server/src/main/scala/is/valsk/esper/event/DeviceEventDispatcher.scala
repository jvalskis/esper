package is.valsk.esper.event

import zio.{Queue, Task, URLayer, ZIO, ZLayer}

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
