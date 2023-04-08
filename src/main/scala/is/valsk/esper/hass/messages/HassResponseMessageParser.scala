package is.valsk.esper.hass.messages

import io.netty.channel.{ChannelFactory, EventLoopGroup}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.TypedMessage.decoder
import is.valsk.esper.hass.messages.responses.*
import is.valsk.esper.hass.messages.{HassMessage, HassResponseMessage, Type}
import is.valsk.esper.repositories.{InMemoryDeviceRepository, Repository}
import zio.*
import zio.http.*
import zio.http.ChannelEvent.UserEvent.{HandshakeComplete, HandshakeTimeout}
import zio.http.ChannelEvent.{ChannelRead, ChannelRegistered, ChannelUnregistered, UserEventTriggered}
import zio.http.model.{Headers, Method}
import zio.http.netty.ChannelFactories.Client
import zio.http.netty.NettyServerConfig
import zio.http.netty.client.NettyClientDriver
import zio.http.socket.{SocketDecoder, SocketProtocol, WebSocketChannelEvent, WebSocketFrame}
import zio.json.*
import zio.nio.channels.*
import zio.nio.{InetAddress, InetSocketAddress}
import zio.stream.ZStream

import java.io.IOException

class HassResponseMessageParser extends MessageParser[HassResponseMessage] {

  def parseMessage(json: String): IO[ParseError, HassResponseMessage] = {
    json.fromJson[TypedMessage] match {
      case Left(value) =>
        ZIO.fail(ParseError(value))
      case Right(Type(messageType)) =>
        val parseResult = messageType match {
          case Type.AuthRequired => json.fromJson[AuthRequired]
          case Type.AuthOK => json.fromJson[AuthOK]
          case Type.AuthInvalid => json.fromJson[AuthInvalid]
          case Type.Result => json.fromJson[Result]
          case other => Left(s"Unsupported type: $other")
        }
        parseResult match
          case Left(value) => ZIO.fail(ParseError(value))
          case Right(value) => ZIO.succeed(value)
      case _ => ZIO.fail(ParseError("Failed to extract HASS message type"))
    }
  }
}

object HassResponseMessageParser {

  val layer: ULayer[MessageParser[HassResponseMessage]] = ZLayer.succeed(HassResponseMessageParser())
}
