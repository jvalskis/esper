package is.valsk.esper.hass.protocol.api

import is.valsk.esper.config.HassConfig
import is.valsk.esper.hass.messages.commands.Auth
import is.valsk.esper.hass.messages.responses.{AuthInvalid, AuthRequired}
import is.valsk.esper.hass.protocol.api.HassResponseMessageHandler.{HassResponseMessageContext, PartialHassResponseMessageHandler}
import zio.*
import zio.http.*
import zio.http.socket.WebSocketFrame
import zio.json.*

class AuthenticationHandler(config: HassConfig) extends HassResponseMessageHandler {

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
      authMessage = Auth(config.accessToken)
      _ <- channel.writeAndFlush(WebSocketFrame.text(authMessage.toJson))
    } yield ()
  }
}

object AuthenticationHandler {
  val layer: URLayer[HassConfig, AuthenticationHandler] = ZLayer {
    for {
      config <- ZIO.service[HassConfig]
    } yield AuthenticationHandler(config)
  }

  val configuredLayer: TaskLayer[AuthenticationHandler] = HassConfig.layer >>> layer
}