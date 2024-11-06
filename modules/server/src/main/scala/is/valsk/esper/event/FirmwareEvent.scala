package is.valsk.esper.event

import is.valsk.esper.domain.{Device, Firmware}

sealed trait FirmwareEvent

case class FirmwareDownloaded(firmware: Firmware) extends FirmwareEvent