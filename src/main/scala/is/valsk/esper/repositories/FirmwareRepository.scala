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

    override def get(key: FirmwareKey): IO[PersistenceException, Option[Firmware]] = for {
      _ <- ZIO.logInfo(s"getting firmware for $key")
      q = quote {
        query[Firmware]
          .filter(_.model == lift(key.model))
          .filter(_.manufacturer == lift(key.manufacturer))
          .filter(_.version == lift(key.version))
          .take(1)
      }
      result <- run(q)
        .map(_.headOption)
        .logError("test")
        .mapError(e => FailedToQueryFirmware(e.getMessage, Some(e)))
    } yield result

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