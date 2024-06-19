package is.valsk.esper.repositories

import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import zio.*

class InMemoryFirmwareRepository(
    map: Ref[Map[FirmwareKey, Firmware]]
) extends FirmwareRepository {

  override def getOpt(id: FirmwareKey): UIO[Option[Firmware]] = map.get.map(_.get(id))

  override def getAll: UIO[List[Firmware]] = map.get.map(_.values.toList)

  override def add(firmware: Firmware): IO[PersistenceException, Firmware] = for {
    _ <- map.update(map => map + (FirmwareKey(firmware) -> firmware))
  } yield firmware

  override def update(firmware: Firmware): IO[PersistenceException, Firmware] = for {
    _ <- map.update(map => map + (FirmwareKey(firmware) -> firmware))
  } yield firmware

  override def getLatestFirmware(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): UIO[Option[Firmware]] = for {
    firmwares <- map.get.map(_.values)
    maybeLatestFirmware = firmwares
      .filter(f => f.manufacturer == manufacturer && f.model == model)
      .maxByOption(_.version)
  } yield maybeLatestFirmware

  override def listVersions(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): UIO[List[Version]] = for {
    firmwares <- map.get.map(_.values)
    versions = firmwares
      .filter(f => f.manufacturer == manufacturer && f.model == model)
      .map(_.version)
      .toList
      .sorted
  } yield versions
}

object InMemoryFirmwareRepository {

  val layer: ULayer[FirmwareRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[FirmwareKey, Firmware])
    } yield InMemoryFirmwareRepository(ref)
  }
}