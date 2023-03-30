package is.valsk.esper.hass

import is.valsk.esper.EsperConfig
import is.valsk.esper.hass.mappers.toDomain
import is.valsk.esper.hass.messages.MessageIdGenerator
import is.valsk.esper.hass.messages.commands.DeviceRegistryList
import is.valsk.esper.hass.messages.responses.Result
import is.valsk.esper.hass.protocol.HassSocketMessageHandler
import is.valsk.esper.model.Device
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.http.socket.{WebSocketChannelEvent, WebSocketFrame}
import zio.json.*

object HassWebsocketClient {
  private val deviceMap = Ref.make[Map[String, Device]](Map.empty)

  private def onConnect(ch: Channel[WebSocketFrame]) = for {
    messageIdRef <- ZIO.service[MessageIdGenerator]
    messageId <- messageIdRef.generate()
    json = DeviceRegistryList(messageId).toJson
    _ <- ZIO.logInfo(s"Sending message $json")
    _ <- ch.writeAndFlush(WebSocketFrame.text(json))
  } yield ()

  private def handleResult(result: Result) = for {
    deviceMapRef <- deviceMap
    _ <- ZIO.foreach(result.result.toSeq.flatten)(hassDevice =>
      hassDevice.toDomain match {
        case Right(domainDevice) =>
          for {
            _ <- deviceMapRef.update(_ + (domainDevice.id -> domainDevice))
            _ <- ZIO.logInfo(s"Updated device registry with device: $domainDevice")
          } yield ()
        case Left(error) =>
          ZIO.logError(s"Failed to convert device to domain model. Error: $error. HASS Device: $hassDevice")
      }
    )
  } yield ()

  private val hassSocketMessageHandler = HassSocketMessageHandler(
    ch => onConnect(ch).provideLayer(MessageIdGenerator.layer)
  ) {
    case result: Result => handleResult(result)
  }

  val hassWebsocketClient: Http[EsperConfig, Throwable, WebSocketChannelEvent, Unit] = Http.collectZIO[WebSocketChannelEvent](hassSocketMessageHandler)

}
