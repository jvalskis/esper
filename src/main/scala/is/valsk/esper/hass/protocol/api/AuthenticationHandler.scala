package is.valsk.esper.hass.protocol.api

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.messages.HassResponseMessage
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.{AuthInvalid, AuthOK, AuthRequired, Result}
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import is.valsk.esper.services.DeviceRepository
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

class AuthenticationHandler(esperConfig: EsperConfig) extends HassResponseMessageHandler {

  override def get: PartialHassResponseMessageHandler = {
    case HassResponseMessageContext(channel, message: AuthInvalid) => handleAuthInvalid(channel, message)
    case HassResponseMessageContext(channel, _: AuthRequired) => handleAuthRequired(channel)
  }

  private def handleAuthInvalid(channel: Channel[WebSocketFrame], message: AuthInvalid): Task[Unit] = for {
    _ <- ZIO.logInfo(s"AuthInvalid: ${message.message}")
    _ <- channel.close()
  } yield ()

  private def handleAuthRequired(channel: Channel[WebSocketFrame]) = {
    for {
      _ <- ZIO.logInfo(s"AuthRequired: sending auth message")
      authMessage = Auth(esperConfig.hassConfig.accessToken)
      _ <- channel.writeAndFlush(WebSocketFrame.text(authMessage.toJson))
    } yield ()
  }
}

object AuthenticationHandler {
  val layer: URLayer[EsperConfig, AuthenticationHandler] = ZLayer {
    for {
      esperConfig <- ZIO.service[EsperConfig]
    } yield AuthenticationHandler(esperConfig)
  }
}