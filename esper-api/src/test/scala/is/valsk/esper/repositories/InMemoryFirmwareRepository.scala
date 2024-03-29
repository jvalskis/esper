package is.valsk.esper.repositories

import is.valsk.esper.EsperConfig
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import zio.*
import zio.stream.{Stream, ZSink, ZStream}

import java.io.IOException
import java.nio.file.StandardOpenOption.{CREATE_NEW, WRITE}
import scala.runtime.Nothing$

class InMemoryFirmwareRepository(
    map: Ref[Map[FirmwareKey, Firmware]]
) extends FirmwareRepository {

  override def get(id: FirmwareKey): UIO[Option[Firmware]] = map.get.map(_.get(id))

  override def getAll: UIO[List[Firmware]] = map.get.map(_.values.toList)

  override def add(firmware: Firmware): IO[FailedToStoreFirmware, Firmware] = for {
    _ <- map.update(map => map + (FirmwareKey(firmware) -> firmware))
  } yield firmware

  override def getLatestFirmware(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): UIO[Option[Firmware]] = for {
    firmwares <- map.get.map(_.values)
    maybeLatestFirmware = firmwares
      .filter(f => f.manufacturer == manufacturer && f.model == model)
      .maxByOption(_.version)
  } yield maybeLatestFirmware
}

object InMemoryFirmwareRepository {

  val layer: URLayer[EsperConfig, FirmwareRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[FirmwareKey, Firmware])
    } yield InMemoryFirmwareRepository(ref)
  }
}