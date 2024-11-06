package is.valsk.esper.event

import zio.Task

trait FirmwareEventListener {

  def onFirmwareEvent(firmwareEvent: FirmwareEvent): Task[Unit]
}
