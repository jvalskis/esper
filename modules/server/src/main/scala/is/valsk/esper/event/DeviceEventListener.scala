package is.valsk.esper.event

import zio.Task

trait DeviceEventListener {

  def onDeviceEvent(deviceEvent: DeviceEvent): Task[Unit]
}
