package is.valsk.esper.services

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.model.Device
import zio.{RIO, *}

trait DeviceRepository {
  def get(id: NonEmptyString): UIO[Option[Device]]

  def list: UIO[List[Device]]

  def add(device: Device): UIO[Unit]
}