package is.valsk.esper.services

import is.valsk.esper.model.Device
import zio.{RIO, *}

trait DeviceRepository {
  def get(id: String): UIO[Option[Device]]

  def list: UIO[List[Device]]

  def add(device: Device): UIO[Unit]
}