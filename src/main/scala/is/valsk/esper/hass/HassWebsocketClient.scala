package is.valsk.esper.hass

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.HassWebsocketClient
import is.valsk.esper.hass.messages.HassMessageParser.parseMessage
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.*
import is.valsk.esper.hass.messages.{HassRequestMessage, HassResponseMessage, MessageIdGenerator}
import is.valsk.esper.hass.protocol.HassSocketMessageHandler
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

object HassWebsocketClient {
  private val deviceMap = Ref.make[Map[String, HassResult]](Map.empty)

  private def onConnect(ch: Channel[WebSocketFrame]) = for {
    messageIdRef <- ZIO.service[MessageIdGenerator]
    messageId <- messageIdRef.generate()
    json = DeviceRegistryList(messageId).toJson
    _ <- ZIO.logInfo(s"Sending message $json")
    _ <- ch.writeAndFlush(WebSocketFrame.text(json))
  } yield ()

  private def handleResult(result: Result) = for {
    _ <- ZIO.logInfo(s"Result: $result")
    deviceMapRef <- deviceMap
    _ <- ZIO.foreach(result.result.toSeq.flatten)(device =>
      for {
        _ <- deviceMapRef.update(_ + (device.id -> device))
        _ <- ZIO.logInfo(s"Updated device registry with device: $device")
      } yield ()
    )
  } yield ()

  private val hassSocketMessageHandler = HassSocketMessageHandler(
    ch => onConnect(ch).provideLayer(MessageIdGenerator.layer)
  ) {
    case result: Result => handleResult(result)
  }

  val hassWebsocketClient: Http[EsperConfig, Throwable, WebSocketChannelEvent, Unit] = Http.collectZIO[WebSocketChannelEvent](hassSocketMessageHandler)

}
