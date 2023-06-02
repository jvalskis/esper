package is.valsk.esper.repositories

import is.valsk.esper.EsperConfig
import is.valsk.esper.domain.{DeviceModel, FailedToStoreFirmware, Firmware, FirmwareDownloadError, FirmwareDownloadFailed}
import is.valsk.esper.domain.Types.Manufacturer
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import zio.*
import zio.nio.file.{Files, Path}
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
}

object InMemoryFirmwareRepository {

  val layer: URLayer[EsperConfig, FirmwareRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[FirmwareKey, Firmware])
    } yield InMemoryFirmwareRepository(ref)
  }
}