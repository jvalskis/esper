package is.valsk.esper.repositories

import is.valsk.esper.domain.{EntityNotFound, PersistenceException}
import zio.*

trait Repository[K, R] {
  def get(id: K): IO[EntityNotFound | PersistenceException, R] = for {
    maybeEntity <- find(id)
    entity <- ZIO
      .fromOption(maybeEntity)
      .mapError(_ => EntityNotFound(id.toString))
  } yield entity

  def find(id: K): IO[PersistenceException, Option[R]]

  def getAll: IO[PersistenceException, List[R]]

  def add(value: R): IO[PersistenceException, R]

  def update(value: R): IO[EntityNotFound | PersistenceException, R]
  
  def delete(id: K): IO[EntityNotFound | PersistenceException, Unit]
}