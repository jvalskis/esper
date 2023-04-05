package is.valsk.esper.repositories

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.domain.Device
import zio.*

trait Repository[K, R] {
  def get(id: K): UIO[Option[R]]

  def list: UIO[List[R]]

  def add(value: R): UIO[Unit]
}