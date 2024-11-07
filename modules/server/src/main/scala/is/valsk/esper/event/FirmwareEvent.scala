package is.valsk.esper.event

import is.valsk.esper.domain.Firmware

sealed trait FirmwareEvent

case class FirmwareDownloaded(firmware: Firmware) extends FirmwareEvent