package is.valsk.esper.hass.protocol.api

import is.valsk.esper.EsperConfig
import is.valsk.esper.device.ManufacturerRegistry
import is.valsk.esper.device.shelly.ShellyDevice
import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.*
import is.valsk.esper.hass.messages.{HassResponseMessage, MessageIdGenerator}
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import is.valsk.esper.hass.protocol.api.{HassResponseMessageHandler, ResultHandler}
import is.valsk.esper.model.Device
import is.valsk.esper.services.DeviceRepository
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

class ResultHandler(
    deviceRepository: DeviceRepository,
    manufacturerRegistry: ManufacturerRegistry
) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = {
    case HassResponseMessageContext(_, result: Result) =>
      ZIO.foreachDiscard(result.result.toSeq.flatten)(hassDevice => manufacturerRegistry.findHandler(hassDevice.manufacturer).flatMap {
        case Some(deviceManufacturerHandler) => deviceManufacturerHandler.toDomain(hassDevice).either.flatMap {
          case Right(domainDevice) =>
            addDeviceToRegistry(domainDevice)
          case Left(error) =>
            ZIO.logError(s"Failed to convert device to domain model. Error: $error. HASS Device: $hassDevice")
        }
        case None => ZIO.logWarning(s"Unsupported manufacturer: ${hassDevice.manufacturer}. HASS Device: $hassDevice")
      })
  }

  private def addDeviceToRegistry(domainDevice: Device): UIO[Unit] = for {
    _ <- deviceRepository.add(domainDevice)
    _ <- ZIO.logInfo(s"Updated device registry with device: $domainDevice")
  } yield ()
}

object ResultHandler {
  val layer: URLayer[DeviceRepository & ManufacturerRegistry, ResultHandler] = ZLayer {
    for {
      deviceRepository <- ZIO.service[DeviceRepository]
      manufacturerRegistry <- ZIO.service[ManufacturerRegistry]
    } yield ResultHandler(deviceRepository, manufacturerRegistry)
  }
}