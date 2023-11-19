package is.valsk.esper.repositories

import eu.timepit.refined.types.string.NonEmptyString
import io.getquill.jdbczio.Quill
import io.getquill.{SnakeCase, *}
import is.valsk.esper.domain.*
import is.valsk.esper.domain.DeviceModel.*
import is.valsk.esper.domain.Types.*
import is.valsk.esper.model.api.PendingUpdate
import is.valsk.esper.model.db.PendingUpdateDto
import zio.*

import java.sql.SQLException

trait PendingUpdateRepository extends Repository[DeviceId, PendingUpdate]

object PendingUpdateRepository {
  private class PendingUpdateRepositoryLive(
      quill: Quill.Postgres[SnakeCase],
      deviceRepository: DeviceRepository,
  ) extends PendingUpdateRepository {

    import quill.*

    given MappedEncoding[NonEmptyString, String] = MappedEncoding[NonEmptyString, String](_.toString)

    given MappedEncoding[String, NonEmptyString] = MappedEncoding[String, NonEmptyString](NonEmptyString.unsafeFrom)

    given MappedEncoding[Version, String] = MappedEncoding[Version, String](_.value)

    given MappedEncoding[String, Version] = MappedEncoding[String, Version](Version(_))

    override def get(key: DeviceId): IO[PersistenceException, PendingUpdate] = {
      getOpt(key)
        .mapError(e => FailedToQueryFirmware(e.getMessage, Some(e)))
        .flatMap(maybeFirmware => ZIO
          .fromOption(maybeFirmware)
          .mapError(_ => EmptyResult())
        )
    }

    override def getOpt(key: DeviceId): IO[PersistenceException, Option[PendingUpdate]] = {
      val q = quote {
        query[PendingUpdateDto]
          .filter(_.id == lift(key))
          .take(1)
      }

      for {
        result <- run(q)
          .map(_.headOption)
          .mapError(e => FailedToQueryFirmware(e.getMessage, Some(e)))
        maybePendingUpdate <- result match {
          case Some(dto) => deviceRepository.get(dto.id).map(device => Some(PendingUpdate(
            device = device,
            version = dto.version
          )))
          case None => ZIO.succeed(None)
        }
      } yield maybePendingUpdate
    }

    override def getAll: IO[PersistenceException, List[PendingUpdate]] = {
      val q = quote {
        query[PendingUpdateDto]
      }
      for {
        result <- run(q)
          .mapError(e => FailedToQueryFirmware(e.getMessage, Some(e)))
        devices <- deviceRepository.getAll.map(_.map(device => device.id -> device).toMap)
        pendingUpdates <- ZIO.foreach(result)(dto => ZIO.succeed(PendingUpdate(
          device = devices(dto.id),
          version = dto.version
        )))
      } yield pendingUpdates
    }

    override def add(pendingUpdate: PendingUpdate): IO[PersistenceException, PendingUpdate] = {
      val q = quote {
        query[PendingUpdateDto]
          .insertValue(lift(map(pendingUpdate)))
          .returning(dto => dto)
      }
      run(q)
        .mapError(e => FailedToStoreFirmware(e.getMessage, DeviceModel(pendingUpdate.device.model, pendingUpdate.device.manufacturer), Some(e)))
        .map(_ => pendingUpdate)
    }

    override def update(value: PendingUpdate): IO[PersistenceException, PendingUpdate] = {
      val q = quote {
        query[PendingUpdateDto]
          .filter(p => p.id == lift(value.device.id))
          .updateValue(lift(map(value)))
          .returning(dto => dto)
      }
      run(q)
        .mapError(e => FailedToStoreFirmware(e.getMessage, DeviceModel(value.device.model, value.device.manufacturer), Some(e)))
        .map(_ => value)
    }

    private def map(value: PendingUpdate): PendingUpdateDto = {
      PendingUpdateDto(
        id = value.device.id,
        version = value.version
      )
    }

  }

  val live: URLayer[Quill.Postgres[SnakeCase] & DeviceRepository, PendingUpdateRepository] = ZLayer.fromFunction(PendingUpdateRepositoryLive(_, _))

}