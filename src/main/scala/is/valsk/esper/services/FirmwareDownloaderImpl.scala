package is.valsk.esper.services

import is.valsk.esper.EsperConfig
import is.valsk.esper.errors.{EsperError, ManufacturerNotSupported}
import is.valsk.esper.model.DeviceModel
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import zio.{IO, ULayer, URLayer, ZIO, ZLayer}

class FirmwareDownloaderImpl(
    manufacturerRegistry: ManufacturerRepository,
    firmwareRepository: FirmwareRepository,
) extends FirmwareDownloader {

  def downloadFirmware(deviceDescriptor: DeviceModel): IO[EsperError, Unit] = for {
    manufacturerHandler <- manufacturerRegistry.get(deviceDescriptor.manufacturer).flatMap {
      case Some(value) => ZIO.succeed(value)
      case None => ZIO.fail(ManufacturerNotSupported(deviceDescriptor.manufacturer))
    }
    firmwareDetails <- manufacturerHandler.getFirmwareDownloadDetails(deviceDescriptor)
    _ <- firmwareRepository.add(firmwareDetails)
    _ <- ZIO.logInfo(s"Firmware downloaded: $firmwareDetails")
  } yield ()

}

object FirmwareDownloaderImpl {

  val layer: URLayer[FirmwareRepository & ManufacturerRepository, FirmwareDownloader] = ZLayer {
    for {
      firmwareRepository <- ZIO.service[FirmwareRepository]
      manufacturerRegistry <- ZIO.service[ManufacturerRepository]
    } yield FirmwareDownloaderImpl(manufacturerRegistry, firmwareRepository)
  }
}
