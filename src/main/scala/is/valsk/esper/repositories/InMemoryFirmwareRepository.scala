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
    esperConfig: EsperConfig,
    map: Ref[Map[FirmwareKey, Firmware]]
) extends FirmwareRepository {

  override def get(id: FirmwareKey): UIO[Option[Firmware]] = map.get.map(_.get(id))

  override def list: UIO[List[Firmware]] = map.get.map(_.values.toList)

  override def add(firmware: Firmware): IO[FailedToStoreFirmware, Firmware] = for {
    _ <- ensureStorageFolderExists(firmware)
      .mapError(e => FailedToStoreFirmware(e.getMessage, firmware.deviceModel, Some(e)))
    _ <- ZStream.fromChunk(firmware.data)
      .run(writeToFile(firmware))
      .mapError(e => FailedToStoreFirmware(e.getMessage, firmware.deviceModel, Some(e)))
    _ <- map.update(map => map + (FirmwareKey(firmware.deviceModel, firmware.version) -> firmware))
  } yield firmware

  def add(firmware: Firmware, stream: Stream[Throwable, Byte]): IO[FirmwareDownloadError, Firmware] = for {
    bytesRead <- stream
      .run(writeToFile(firmware))
      .mapError(e => FirmwareDownloadFailed(e.getMessage, firmware.deviceModel, Some(e)))
    updatedFirmware = firmware.copy(size = bytesRead)
    _ <- map.update(map => map + (FirmwareKey(firmware.deviceModel, firmware.version) -> updatedFirmware))
  } yield updatedFirmware

  private def writeToFile(firmwareDetails: Firmware): ZSink[Any, Throwable, Byte, Byte, Long] = {
    ZSink.fromFile(
      getFile(firmwareDetails).toFile,
      options = Set(CREATE_NEW, WRITE)
    )
  }

  private def ensureStorageFolderExists(firmwareDetails: Firmware): IO[IOException, Unit] = {
    Files.createDirectories(getFirmwareDirectory(firmwareDetails))
  }

  private def getFile(firmware: Firmware): Path = getFirmwareDirectory(firmware) / firmware.deviceModel.model.toString

  private def getFirmwareDirectory(firmware: Firmware): Path = Path(
    esperConfig.firmwareStoragePath,
    firmware.deviceModel.manufacturer.toString,
    firmware.version.value,
  )

}

object InMemoryFirmwareRepository {

  val layer: URLayer[EsperConfig, FirmwareRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[FirmwareKey, Firmware])
      esperConfig <- ZIO.service[EsperConfig]
    } yield InMemoryFirmwareRepository(esperConfig, ref)
  }
}