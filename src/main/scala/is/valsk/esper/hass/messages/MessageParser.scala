package is.valsk.esper.hass.messages

import io.circe.*
import io.circe.parser.*
import io.netty.channel.{ChannelFactory, EventLoopGroup}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.commands.{Auth, DeviceRegistryList}
import is.valsk.esper.hass.messages.responses.{AuthInvalid, AuthOK, AuthRequired, Result}
import is.valsk.esper.hass.messages.{HassMessage, HassResponseMessage, Type}
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

trait MessageParser[T] {

  def parseMessage(json: String): IO[ParseError, T]
}

object MessageParser {

  case class ParseError(message: String, underlying: Option[Throwable] = None) extends Exception(message, underlying.orNull)
}