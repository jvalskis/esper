package is.valsk.esper.hass.protocol

import is.valsk.esper.hass.protocol.ChannelHandler.PartialChannelHandler
import zio.*
import zio.http.*
import zio.http.ChannelEvent.*
import zio.json.*

class UnhandledMessageHandler extends ChannelHandler {

  override def get: PartialChannelHandler = {
    case message => ZIO.logInfo(s"Unhandled: $message")
  }
}

object UnhandledMessageHandler {

  val layer: ULayer[UnhandledMessageHandler] = ZLayer.succeed(UnhandledMessageHandler())
}