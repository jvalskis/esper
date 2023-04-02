package is.valsk.esper.hass.protocol.api

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.mappers.toDomain
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.{AuthInvalid, AuthOK, AuthRequired, Result}
import is.valsk.esper.hass.messages.{HassResponseMessage, MessageIdGenerator}
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import is.valsk.esper.hass.protocol.api.{HassResponseMessageHandler, ResultHandler}
import is.valsk.esper.services.DeviceRepository
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

class ResultHandler(
    deviceRepository: DeviceRepository,
) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = {
    case HassResponseMessageContext(_, result: Result) =>
      for {
        _ <- ZIO.foreach(result.result.toSeq.flatten)(hassDevice =>
          hassDevice.toDomain match {
            case Right(domainDevice) =>
              for {
                _ <- deviceRepository.add(domainDevice)
                _ <- ZIO.logInfo(s"Updated device registry with device: $domainDevice")
              } yield ()
            case Left(error) =>
              ZIO.logError(s"Failed to convert device to domain model. Error: $error. HASS Device: $hassDevice")
          }
        )
      } yield ()
  }
}

object ResultHandler {
  val layer: URLayer[DeviceRepository, ResultHandler] = ZLayer {
    for {
      deviceRepository <- ZIO.service[DeviceRepository]
    } yield ResultHandler(deviceRepository)
  }
}