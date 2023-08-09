package is.valsk.esper.hass.messages

import is.valsk.esper.hass.messages.MessageParser.ParseError
import zio.*

trait MessageParser[T] {

  def parseMessage(json: String): IO[ParseError, T]
}

object MessageParser {

  case class ParseError(message: String, underlying: Option[Throwable] = None) extends Exception(message, underlying.orNull)
}