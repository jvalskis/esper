package is.valsk.esper.hass.protocol.api

import is.valsk.esper.hass.messages.MessageIdGenerator
import is.valsk.esper.hass.messages.commands.DeviceRegistryList
import is.valsk.esper.hass.messages.responses.AuthOK
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import zio.*
import zio.http.*
import zio.http.socket.WebSocketFrame
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