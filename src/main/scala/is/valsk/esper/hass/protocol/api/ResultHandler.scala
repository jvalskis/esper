package is.valsk.esper.hass.protocol.api

import is.valsk.esper.EsperConfig
import is.valsk.esper.device.ManufacturerRepository
import is.valsk.esper.device.shelly.ShellyDevice
import is.valsk.esper.errors.ManufacturerNotSupported
import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.*
import is.valsk.esper.hass.messages.{HassResponseMessage, MessageIdGenerator}
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import is.valsk.esper.hass.protocol.api.{HassResponseMessageHandler, ResultHandler}
import is.valsk.esper.model.Device
import is.valsk.esper.services.{DeviceRepository, Repository}
import is.valsk.esper.types.Manufacturer
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

class ResultHandler(
    deviceRepository: DeviceRepository,
    manufacturerRegistry: ManufacturerRepository
) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = {
    case HassResponseMessageContext(_, result: Result) =>
      ZIO.foreachDiscard(result.result.toSeq.flatten)(hassDevice =>
        val result = for {
          manufacturer <- ZIO.fromEither(Manufacturer.from(hassDevice.manufacturer)).mapError(ParseError(_))
          result <- manufacturerRegistry.get(manufacturer).flatMap {
            case Some(deviceManufacturerHandler) => deviceManufacturerHandler.toDomain(hassDevice).either.flatMap {
              case Right(domainDevice) =>
                addDeviceToRegistry(domainDevice)
              case Left(error) =>
                ZIO.fail(ParseError(error))
            }
            case None => ZIO.fail(ManufacturerNotSupported(manufacturer))
          }
        } yield result
        result.catchAll(error => ZIO.logError(s"Failed to handle device. Error: ${error.getMessage}. HASS Device: $hassDevice"))
      )
  }

  private def addDeviceToRegistry(domainDevice: Device): UIO[Unit] = for {
    _ <- deviceRepository.add(domainDevice)
    _ <- ZIO.logInfo(s"Updated device registry with device: $domainDevice")
  } yield ()
}

object ResultHandler {
  val layer: URLayer[DeviceRepository & ManufacturerRepository, ResultHandler] = ZLayer {
    for {
      deviceRepository <- ZIO.service[DeviceRepository]
      manufacturerRegistry <- ZIO.service[ManufacturerRepository]
    } yield ResultHandler(deviceRepository, manufacturerRegistry)
  }
}