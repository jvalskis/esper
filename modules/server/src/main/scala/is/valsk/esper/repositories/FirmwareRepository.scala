package is.valsk.esper.repositories

import eu.timepit.refined.types.string.NonEmptyString
import io.getquill.*
import io.getquill.jdbczio.Quill
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.*
import is.valsk.esper.event.{FirmwareDownloaded, FirmwareEventProducer}
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import zio.*

import java.sql.SQLException

trait FirmwareRepository extends Repository[FirmwareKey, Firmware] {

  def getLatestFirmware(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): IO[PersistenceException, Option[Firmware]]

  def listVersions(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): IO[PersistenceException, List[Version]]
}

object FirmwareRepository {
  private class FirmwareRepositoryLive(
      quill: Quill.Postgres[SnakeCase],
      firmwareEventProducer: FirmwareEventProducer,
  ) extends FirmwareRepository {

    import quill.*

    given MappedEncoding[NonEmptyString, String] = MappedEncoding[NonEmptyString, String](_.toString)

    given MappedEncoding[String, NonEmptyString] = MappedEncoding[String, NonEmptyString](NonEmptyString.unsafeFrom)

    given MappedEncoding[Version, String] = MappedEncoding[Version, String](_.value)

    given MappedEncoding[String, Version] = MappedEncoding[String, Version](Version(_))

    override def find(key: FirmwareKey): IO[PersistenceException, Option[Firmware]] = {
      val q = quote {
        query[Firmware]
          .filter(_.model == lift(key.model))
          .filter(_.manufacturer == lift(key.manufacturer))
          .filter(_.version == lift(key.version))
          .take(1)
      }
      run(q)
        .map(_.headOption)
        .mapError(e => PersistenceException(e.getMessage, Some(e)))
    }

    override def getAll: IO[PersistenceException, List[Firmware]] = {
      val q = quote {
        query[Firmware]
      }
      run(q)
        .mapError(e => PersistenceException(e.getMessage, Some(e)))
    }

    override def add(firmware: Firmware): IO[PersistenceException, Firmware] = {
      val q = quote {
        query[Firmware]
          .insertValue(lift(firmware))
          .returning(fw => fw)
      }
      for {
        persistedFirmware <- run(q)
          .mapError(e => PersistenceException(e.getMessage, Some(e)))
          .map(_ => firmware)
        _ <- firmwareEventProducer.produceEvent(FirmwareDownloaded(firmware))
      } yield firmware
    }

    override def getLatestFirmware(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): IO[PersistenceException, Option[Firmware]] = for {
      _ <- ZIO.logInfo(s"getting latest firmware for $manufacturer $model")
      maybeLatestVersion <- listVersions(manufacturer, model)
        .map(_.maxOption)
      maybeLatestFirmware <- maybeLatestVersion match {
        case Some(version) => find(FirmwareKey(manufacturer, model, version))
        case None => ZIO.succeed(None)
      }
    } yield maybeLatestFirmware

    override def listVersions(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): IO[PersistenceException, List[Version]] = {
      val q = quote {
        query[Firmware]
          .filter(_.model == lift(model))
          .filter(_.manufacturer == lift(manufacturer))
          .map(_.version)
      }
      run(q)
        .mapError(e => PersistenceException(e.getMessage, Some(e)))
        .map(_.sorted)
    }

    override def update(value: Firmware): IO[PersistenceException, Firmware] = ???
    
    override def delete(key: FirmwareKey): IO[PersistenceException, Unit] = ???
  }

  val live: URLayer[Quill.Postgres[SnakeCase] & FirmwareEventProducer, FirmwareRepository] = ZLayer.fromFunction(FirmwareRepositoryLive(_, _))

  case class FirmwareKey(
      manufacturer: Manufacturer,
      model: Model,
      version: Version
  )

  object FirmwareKey {
    def apply(firmware: Firmware): FirmwareKey = FirmwareKey(firmware.manufacturer, firmware.model, firmware.version)
  }

}