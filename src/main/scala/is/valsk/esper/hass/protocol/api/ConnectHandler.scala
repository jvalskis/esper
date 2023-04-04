package is.valsk.esper.hass.protocol.api

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.{AuthInvalid, AuthOK, AuthRequired, Result}
import is.valsk.esper.hass.messages.{HassResponseMessage, MessageIdGenerator}
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import is.valsk.esper.services.Repository
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

class ConnectHandler(
    messageIdGenerator: MessageIdGenerator,
) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = {
    case HassResponseMessageContext(channel, _: AuthOK) =>
      for {
        messageId <- messageIdGenerator.generate()
        json = DeviceRegistryList(messageId).toJson
        _ <- ZIO.logInfo(s"Sending message $json")
        _ <- channel.writeAndFlush(WebSocketFrame.text(json))
      } yield ()
  }
}

object ConnectHandler {

  val layer: URLayer[MessageIdGenerator, ConnectHandler] = ZLayer {
    for {
      messageIdGenerator <- ZIO.service[MessageIdGenerator]
    } yield ConnectHandler(messageIdGenerator)
  }
}