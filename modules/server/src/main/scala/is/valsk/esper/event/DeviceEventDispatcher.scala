package is.valsk.esper.event

import zio.{Queue, Ref, Task, URLayer, ZIO, ZLayer}

object DeviceEventDispatcher {

  private class DeviceEventDispatcherLive(
      eventQueue: Queue[DeviceEvent],
      listeners: Seq[DeviceEventListener],
  ) extends EventDispatcher[DeviceEvent, DeviceEventListener] {
    override def invokeListener(event: DeviceEvent)(listener: DeviceEventListener): Task[Unit] = listener.onDeviceEvent(event)
  }

  val layer: URLayer[Seq[DeviceEventListener] & Queue[DeviceEvent], EventDispatcher[DeviceEvent, DeviceEventListener]] = ZLayer.fromFunction(DeviceEventDispatcherLive(_, _))
}
