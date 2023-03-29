package is.valsk.esper

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

object WebSocketSimpleClient extends ZIOAppDefault {

  val httpSocket: Http[EsperConfig, Throwable, WebSocketChannelEvent, Unit] = Http.collectZIO[WebSocketChannelEvent] {
    case ChannelEvent(_, UserEventTriggered(event)) =>
      event match {
        case HandshakeComplete => ZIO.logInfo("Connection started!")
        case HandshakeTimeout => ZIO.logInfo("Connection failed!")
      }

    case ChannelEvent(_, ChannelRegistered) =>
      ZIO.logInfo("Connection opened!")

    case ChannelEvent(_, ChannelUnregistered) =>
      ZIO.logInfo("Connection closed!")

    case ChannelEvent(ch, ChannelRead(WebSocketFrame.Ping)) => for {
      _ <- ZIO.logInfo("Received PING - sending PONG")
      _ <- ch.writeAndFlush(WebSocketFrame.Pong)
    } yield ()

    case ChannelEvent(ch, ChannelRead(WebSocketFrame.Text(json))) =>
      val log: PartialFunction[HassResponseMessage, RIO[EsperConfig, Unit]] = {
        case x => ZIO.logWarning(s"Message not handled: $x")
      }
      val pf: PartialFunction[HassResponseMessage, RIO[EsperConfig, Unit]] = {
        case Result(_, id) => ZIO.logInfo(s"Result: $id")
      }
      val authHandler: PartialFunction[HassResponseMessage, RIO[EsperConfig, Unit]] = {
        case AuthInvalid(_, message) => for {
          _ <- ZIO.logInfo(s"AuthInvalid: $message")
          _ <- ch.close()
        } yield ()
        case AuthOK(_, _) => for {
          _ <- ZIO.logInfo(s"AuthOK")
          _ <- ch.writeAndFlush(WebSocketFrame.text(DeviceRegistryList(1).toJson))
        } yield ()
        case AuthRequired(_, _) => for {
          _ <- ZIO.logInfo(s"AuthRequired: sending auth message")
          hassConfig <- EsperConfig.hassConfig
          authMessage = Auth(hassConfig.accessToken)
          _ <- ch.writeAndFlush(WebSocketFrame.text(authMessage.toJson))
        } yield ()
      }
      val result = for {
        parsedMessage <- parseMessage(json)
        _ <- (authHandler orElse pf orElse log).apply(parsedMessage)
      } yield ()
      result
        .catchAll(e => ZIO.logError(s"Failed to parse message: ${e.getMessage}. Message: $json"))
    case x => Console.printLine(s"Unhandled: $x")
  }

  private val app = ZIO.service[EsperConfig].flatMap(
    config => httpSocket
      .provideLayer(EsperConfig.layer)
      .toSocketApp
      .connect(config.hassConfig.webSocketUrl) *> ZIO.never
  )

  val run = app.provide(
    Client.default,
    Scope.default,
    EsperConfig.layer
  )

}