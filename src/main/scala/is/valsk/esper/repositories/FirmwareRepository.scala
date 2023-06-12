package is.valsk.esper.repositories

import eu.timepit.refined.types.string.NonEmptyString
import io.getquill.context.sql.idiom.SqlIdiom
import io.getquill.jdbczio.{Quill, QuillBaseContext}
import io.getquill.{SnakeCase, *}
import is.valsk.esper.EsperConfig
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.*
import is.valsk.esper.domain.DeviceModel.*
import is.valsk.esper.domain.Types.*
import is.valsk.esper.domain.Types.NonEmptyStringImplicits.*
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import zio.*
import zio.json.*
import zio.stream.Stream

import java.sql.SQLException
import java.util.Base64
import javax.sql.DataSource

type FirmwareRepository = FirmwareRepository2

trait FirmwareRepository2 extends Repository[FirmwareKey, Firmware] {

  def getLatestFirmware(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): IO[PersistenceException, Option[Firmware]]

  def listVersions(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): IO[PersistenceException, List[Version]]
}

object FirmwareRepository {
  private class FirmwareRepositoryLive(
      quill: Quill.Postgres[SnakeCase],
  ) extends FirmwareRepository {

    import quill.*

    given MappedEncoding[NonEmptyString, String] = MappedEncoding[NonEmptyString, String](_.toString)

    given MappedEncoding[String, NonEmptyString] = MappedEncoding[String, NonEmptyString](NonEmptyString.unsafeFrom)

    given MappedEncoding[Version, String] = MappedEncoding[Version, String](_.value)

    given MappedEncoding[String, Version] = MappedEncoding[String, Version](Version(_))

    override def get(key: FirmwareKey): IO[PersistenceException, Firmware] = {
      getOpt(key)
        .mapError(e => FailedToQueryFirmware(e.getMessage, Some(e)))
        .logError("test")
        .flatMap(maybeFirmware => ZIO
          .fromOption(maybeFirmware)
          .mapError(_ => EmptyResult())
        )
    }

    override def getOpt(key: FirmwareKey): IO[PersistenceException, Option[Firmware]] = {
      val q = quote {
        query[Firmware]
          .filter(_.model == lift(key.model))
          .filter(_.manufacturer == lift(key.manufacturer))
          .filter(_.version == lift(key.version))
          .take(1)
      }
      run(q)
        .map(_.headOption)
        .logError("test")
        .mapError(e => FailedToQueryFirmware(e.getMessage, Some(e)))
    }

    override def getAll: IO[PersistenceException, List[Firmware]] = {
      val q = quote {
        query[Firmware]
      }
      run(q)
        .mapError(e => FailedToQueryFirmware(e.getMessage, Some(e)))
    }

    override def add(firmware: Firmware): IO[PersistenceException, Firmware] = {
      val q = quote {
        query[Firmware]
          .insertValue(lift(firmware))
          .returning(fw => fw)
      }
      run(q)
        .mapError(e => FailedToStoreFirmware(e.getMessage, DeviceModel(firmware), Some(e)))
        .map(_ => firmware)
    }

    override def getLatestFirmware(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): IO[PersistenceException, Option[Firmware]] = for {
      _ <- ZIO.logInfo(s"getting latest firmware for $manufacturer $model")
      maybeLatestVersion <- listVersions(manufacturer, model)
        .map(_.maxOption)
      maybeLatestFirmware <- maybeLatestVersion match {
        case Some(version) => getOpt(FirmwareKey(manufacturer, model, version))
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
        .logError("test")
        .mapError(e => FailedToQueryFirmware(e.getMessage, Some(e)))
        .map(_.sorted)
    }
  }

  val live: URLayer[Quill.Postgres[SnakeCase], FirmwareRepository] = ZLayer.fromFunction(FirmwareRepositoryLive(_))

  case class FirmwareKey(
      manufacturer: Manufacturer,
      model: Model,
      version: Version
  )

  object FirmwareKey {
    def apply(firmware: Firmware): FirmwareKey = FirmwareKey(firmware.manufacturer, firmware.model, firmware.version)
  }

}