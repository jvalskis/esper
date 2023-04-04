package is.valsk.esper.services

import is.valsk.esper.EsperConfig
import is.valsk.esper.device.{DeviceDescriptor, ManufacturerRepository}
import is.valsk.esper.errors.{EsperError, ManufacturerNotSupported}
import zio.{IO, ULayer, URLayer, ZIO, ZLayer}

class FirmwareDownloaderImpl(
    esperConfig: EsperConfig,
    manufacturerRegistry: ManufacturerRepository
) extends FirmwareDownloader {

  def downloadFirmware(deviceDescriptor: DeviceDescriptor): IO[EsperError, Unit] = for {
    manufacturerHandler <- manufacturerRegistry.get(deviceDescriptor.manufacturer).flatMap {
      case Some(value) => ZIO.succeed(value)
      case None => ZIO.fail(ManufacturerNotSupported(deviceDescriptor.manufacturer))
    }
    firmware <- manufacturerHandler.downloadFirmware(deviceDescriptor)
    _ <- ZIO.logInfo(s"Firmware downloaded: $firmware")
  } yield ()

  def hasLatestFirmware(deviceDescriptor: DeviceDescriptor): Boolean = ???

}

object FirmwareDownloaderImpl {

  val layer: URLayer[EsperConfig & ManufacturerRepository, FirmwareDownloader] = ZLayer {
    for {
      esperConfig <- ZIO.service[EsperConfig]
      manufacturerRegistry <- ZIO.service[ManufacturerRepository]
    } yield FirmwareDownloaderImpl(esperConfig, manufacturerRegistry)
  }
}
