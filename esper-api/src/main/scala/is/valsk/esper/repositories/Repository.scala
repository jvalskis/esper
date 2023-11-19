package is.valsk.esper.repositories

import is.valsk.esper.domain.PersistenceException
import zio.*

trait Repository[K, R] {
  def get(id: K): IO[PersistenceException, R]

  def getOpt(id: K): IO[PersistenceException, Option[R]]

  def getAll: IO[PersistenceException, List[R]]

  def add(value: R): IO[PersistenceException, R]

  def update(value: R): IO[PersistenceException, R]
}