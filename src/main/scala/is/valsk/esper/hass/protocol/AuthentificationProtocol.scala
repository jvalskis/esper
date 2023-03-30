package is.valsk.esper.hass.protocol

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.messages.HassMessageParser.parseMessage
import is.valsk.esper.hass.messages.HassResponseMessage
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.{AuthInvalid, AuthOK, AuthRequired, Result}
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

class AuthentificationProtocol(
    channel: Channel[WebSocketFrame],
    onConnect: Function[Channel[WebSocketFrame], RIO[EsperConfig, Unit]]
) extends PartialFunction[HassResponseMessage, RIO[EsperConfig, Unit]] {

  private val pf: PartialFunction[HassResponseMessage, RIO[EsperConfig, Unit]] = {
    case message: AuthInvalid => handleAuthInvalid(message).provide(EsperConfig.layer)
    case message: AuthOK => handleAuthOK(message)
    case message: AuthRequired => handleAuthRequired(message)
  }

  private def handleAuthOK(message: AuthOK):RIO[EsperConfig, Unit] = {
    for {
      _ <- ZIO.logInfo(s"AuthOK")
      _ <- onConnect(channel)
    } yield ()
  }

  private def handleAuthInvalid(message: AuthInvalid): RIO[EsperConfig, Unit] = for {
    _ <- ZIO.logInfo(s"AuthInvalid: ${message.message}")
    _ <- channel.close()
  } yield ()

  private def handleAuthRequired(message: AuthRequired) = {
    for {
      _ <- ZIO.logInfo(s"AuthRequired: sending auth message")
      hassConfig <- EsperConfig.hassConfig
      authMessage = Auth(hassConfig.accessToken)
      _ <- channel.writeAndFlush(WebSocketFrame.text(authMessage.toJson))
    } yield ()
  }

  override def isDefinedAt(message: HassResponseMessage): Boolean = pf.isDefinedAt(message)

  override def apply(message: HassResponseMessage): RIO[EsperConfig, Unit] = pf.apply(message)
}
