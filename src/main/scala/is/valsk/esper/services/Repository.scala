package is.valsk.esper.services

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.model.Device
import zio.{RIO, *}

trait Repository[K, R] {
  def get(id: K): UIO[Option[R]]

  def list: UIO[List[R]]

  def add(value: R): UIO[Unit]
}