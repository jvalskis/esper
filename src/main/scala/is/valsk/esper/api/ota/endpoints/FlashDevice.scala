package is.valsk.esper.api.ota.endpoints

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.{DeviceProxy, DeviceProxyRegistry}
import is.valsk.esper.domain.Version.encoder
import is.valsk.esper.domain.*
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.{DeviceRepository, FirmwareRepository, ManufacturerRepository}
import zio.http.Response
import zio.http.model.HttpError.NotFound
import zio.http.model.{HttpError, Status}
import zio.json.*
import zio.{IO, URLayer, ZIO, ZLayer}

class FlashDevice(
    deviceProxyRegistry: DeviceProxyRegistry,
    deviceRepository: DeviceRepository,
    firmwareRepository: FirmwareRepository,
    manufacturerRepository: ManufacturerRepository
) {

  def apply(deviceId: NonEmptyString, maybeVersion: Option[Version]): IO[HttpError, Response] = {
    for {
      device <- deviceRepository.get(deviceId)
      firmware <- maybeVersion match {
        case Some(version) =>
          firmwareRepository.get(FirmwareKey(device.manufacturer, device.model, version))
        case None => for {
          device <- deviceRepository.get(deviceId)
          manufacturerHandler <- manufacturerRepository.get(device.manufacturer)
          latestFirmware <- firmwareRepository.getLatestFirmware(device.manufacturer, device.model)(using manufacturerHandler.versionOrdering)
            .flatMap {
              case None => ZIO.fail(NotFound("")) // TODO error handling
              case Some(result) => ZIO.succeed(result)
            }
        } yield latestFirmware
      }
      deviceProxy <- deviceProxyRegistry.selectProxy(device.manufacturer)
      _ <- deviceProxy.flashFirmware(device, firmware)
    } yield Response.ok
  }
    .mapError {
      case e@MalformedVersion(version, device) => HttpError.BadRequest(e.getMessage)
      case e@ApiCallFailed(message, device, cause) => HttpError.BadGateway(e.getMessage)
      case e@ManufacturerNotSupported(manufacturer) => HttpError.PreconditionFailed(e.getMessage)
      case e@FailedToParseApiResponse(message, device, cause) => HttpError.BadGateway(e.getMessage)
    }
}

object FlashDevice {

  val layer: URLayer[DeviceProxyRegistry & DeviceRepository & FirmwareRepository & ManufacturerRepository, FlashDevice] = ZLayer.fromFunction(FlashDevice(_, _, _, _))

}
