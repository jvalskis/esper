package is.valsk.esper.services

import is.valsk.esper.EsperConfig
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.Types.UrlString
import is.valsk.esper.domain.*
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import zio.nio.file.{Files, Path}
import zio.stream.{ZChannel, ZSink, ZStream}
import zio.{IO, ULayer, URLayer, ZIO, ZLayer}

import java.io.IOException
import java.nio.file.StandardOpenOption.{CREATE, CREATE_NEW, TRUNCATE_EXISTING, WRITE}

class FirmwareDownloaderImpl(
    manufacturerRegistry: ManufacturerRepository,
    firmwareRepository: FirmwareRepository,
    httpClient: HttpClient
) extends FirmwareDownloader {

  def downloadFirmware(deviceModel: DeviceModel): IO[EsperError, Firmware] = for {
    manufacturerHandler <- manufacturerRegistry.get(deviceModel.manufacturer).flatMap {
      case Some(value) => ZIO.succeed(value)
      case None => ZIO.fail(ManufacturerNotSupported(deviceModel.manufacturer))
    }
    firmwareDetails <- manufacturerHandler.getFirmwareDownloadDetails(deviceModel)
    bytes <- httpClient.download(firmwareDetails.url.toString)
      .run(ZSink.collectAll[Byte])
      .mapError(e => FirmwareDownloadFailed(e.getMessage, deviceModel, Some(e)))
    _ <- ZIO.logInfo(s"Firmware downloaded: $deviceModel. Bytes read: ${bytes.size}")
    result <- firmwareRepository.add(Firmware(
      deviceModel = firmwareDetails.deviceModel,
      version = firmwareDetails.version.value,
      data = bytes,
      size = bytes.size
    ))
  } yield result
}

object FirmwareDownloaderImpl {

  val layer: URLayer[HttpClient & FirmwareRepository & ManufacturerRepository, FirmwareDownloader] = ZLayer {
    for {
      httpClient <- ZIO.service[HttpClient]
      firmwareRepository <- ZIO.service[FirmwareRepository]
      manufacturerRegistry <- ZIO.service[ManufacturerRepository]
    } yield FirmwareDownloaderImpl(manufacturerRegistry, firmwareRepository, httpClient)
  }
}
