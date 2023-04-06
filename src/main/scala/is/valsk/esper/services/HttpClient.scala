package is.valsk.esper.services

import is.valsk.esper.hass.messages.MessageParser.ParseError
import zio.http.{Client, Response}
import zio.{URLayer, ZIO, ZLayer}
import zio.json.*

class HttpClient {

  def get(url: String): ZIO[Any, Throwable, Response] = {
    Client.request(url)
      .provide(Client.default)
  }

  def getJson[T](url: String)(using JsonDecoder[T]): ZIO[Any, Throwable, T] = {
    Client.request(url)
      .provide(Client.default)
      .flatMap(_.body.asString)
      .flatMap(response => ZIO
        .fromEither(response.fromJson[T])
        .mapError(ParseError(_))
      )
  }
}

object HttpClient {

  val layer: URLayer[Client, HttpClient] = ZLayer {
    for {
      client <- ZIO.service[Client]
    } yield HttpClient()
  }
}