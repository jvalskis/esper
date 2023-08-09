package is.valsk.esper.services

import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import is.valsk.esper.services.FirmwareService.LatestFirmwareStatus
import is.valsk.esper.services.FirmwareService.LatestFirmwareStatus.LatestFirmwareStatus
import zio.{IO, URLayer, ZIO, ZLayer}

class FirmwareService(
    firmwareRepository: FirmwareRepository,
    manufacturerRepository: ManufacturerRepository,
) {

  def getFirmware(manufacturer: Manufacturer, model: Model, version: Version): IO[EsperError, Firmware] = {
    firmwareRepository
      .get(FirmwareKey(manufacturer, model, version))
      .mapError {
        case EmptyResult() => FirmwareNotFound("Firmware not found", manufacturer, model, Some(version))
        case x => x
      }
  }

  def getLatestFirmware(manufacturer: Manufacturer, model: Model): IO[EsperError, Firmware] = for {
    manufacturerHandler <- manufacturerRepository.get(manufacturer)
    latestFirmware <- firmwareRepository.getLatestFirmware(manufacturer, model)(using manufacturerHandler.versionOrdering)
      .flatMap {
        case None => ZIO.fail(FirmwareNotFound("Latest firmware not found", manufacturer, model, None))
        case Some(result) => ZIO.succeed(result)
      }
  } yield latestFirmware

  def latestFirmwareStatus(device: Device): IO[EsperError, LatestFirmwareStatus] = for {
    manufacturerHandler <- manufacturerRepository.get(device.manufacturer)
    maybeLatestFirmware <- firmwareRepository.getLatestFirmware(device.manufacturer, device.model)(using manufacturerHandler.versionOrdering)
    maybeCurrentVersion <- device.softwareVersion
      .map(manufacturerHandler.parseVersion)
      .fold(ZIO.succeed(Option.empty[Version])) {
        case Left(error) => ZIO.fail(error)
        case Right(version) => ZIO.succeed(Some(version))
      }
    status = (maybeCurrentVersion, maybeLatestFirmware.map(_.version)) match {
      case (None, _) => LatestFirmwareStatus.Undefined
      case (_, None) => LatestFirmwareStatus.Undefined
      case (Some(currentVersion), Some(latestFirmwareVersion)) =>
        given ordering: Ordering[Version] = manufacturerHandler.versionOrdering

        if (latestFirmwareVersion > currentVersion)
          LatestFirmwareStatus.Outdated(latestFirmwareVersion)
        else
          LatestFirmwareStatus.Latest
    }
  } yield status
}

object FirmwareService {
  val layer: URLayer[FirmwareRepository & ManufacturerRepository, FirmwareService] = ZLayer.fromFunction(FirmwareService(_, _))

  object LatestFirmwareStatus {
    sealed trait LatestFirmwareStatus

    case object Latest extends LatestFirmwareStatus

    case class Outdated(newVersion: Version) extends LatestFirmwareStatus

    case object Undefined extends LatestFirmwareStatus
  }
}
