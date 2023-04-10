package is.valsk.esper.repositories

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.domain.{Device, PersistenceException}
import zio.*

trait Repository[K, R] {
  def get(id: K): IO[PersistenceException, Option[R]]

  def getAll: IO[PersistenceException, List[R]]

  def add(value: R): IO[PersistenceException, R]
}