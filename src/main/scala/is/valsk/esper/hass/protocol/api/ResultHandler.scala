package is.valsk.esper.hass.protocol.api

import is.valsk.esper.device.DeviceManufacturerHandler
import is.valsk.esper.domain.Types.Manufacturer
import is.valsk.esper.domain.{Device, EsperError, ManufacturerIsEmpty, ManufacturerNotSupported}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.*
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import is.valsk.esper.repositories.{DeviceRepository, ManufacturerRepository}
import is.valsk.esper.services.PendingUpdateService
import zio.*
import zio.http.*

class ResultHandler(
    deviceRepository: DeviceRepository,
    manufacturerRegistry: ManufacturerRepository,
    pendingUpdateService: PendingUpdateService,
) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = {
    case HassResponseMessageContext(_, result: Result) =>
      ZIO.foreachDiscard(result.result.toSeq.flatten)(hassDevice =>
        val result = for {
          manufacturer <- ZIO.fromEither(hassDevice.manufacturer.map(Manufacturer.from).getOrElse(Left(()))).mapError(_ => ManufacturerIsEmpty())
          result <- manufacturerRegistry.getOpt(manufacturer).flatMap {
            case Some(deviceManufacturerHandler) => deviceManufacturerHandler.toDomain(hassDevice).either.flatMap {
              case Right(domainDevice) =>
                addDeviceToRegistry(domainDevice)
              case Left(error) =>
                ZIO.fail(error)
            }
            case None => ZIO.fail(ManufacturerNotSupported(manufacturer))
          }
        } yield result
        result.catchAll {
          case e: ManufacturerNotSupported =>
            ZIO.logWarning(s"${e.getMessage}. HASS Device: $hassDevice")
          case e: ManufacturerIsEmpty =>
            ZIO.logWarning(s"${e.getMessage}. HASS Device: $hassDevice")
          case error =>
            ZIO.logError(s"Failed to handle device. Error: ${error.getMessage}. HASS Device: $hassDevice")
        }
      )
  }

  private def addDeviceToRegistry(domainDevice: Device): IO[EsperError, Unit] = for {
    _ <- deviceRepository.add(domainDevice)
    _ <- pendingUpdateService.deviceAdded(domainDevice)
    _ <- ZIO.logInfo(s"Updated device registry with device: $domainDevice")
  } yield ()
}

object ResultHandler {
  val layer: URLayer[PendingUpdateService & DeviceRepository & ManufacturerRepository, ResultHandler] = ZLayer.fromFunction(ResultHandler(_, _, _))
}