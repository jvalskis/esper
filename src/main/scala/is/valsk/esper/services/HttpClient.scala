package is.valsk.esper.services

import is.valsk.esper.hass.messages.MessageParser.ParseError
import zio.http.{Client, Request, Response, URL}
import zio.{URLayer, ZIO, ZLayer}
import zio.json.*
import zio.stream.*

import java.io.IOException

class HttpClient {

  def get(url: String): ZIO[Any, Throwable, Response] = request(url)

  def get(req: Request): ZIO[Any, Throwable, Response] = request(req)

  def download(url: String): Stream[Throwable, Byte] = ZStream.fromZIO(get(url).map(_.body.asStream)).flatten

  def getJson[T](req: Request)(using JsonDecoder[T]): ZIO[Any, Throwable, T] = {
    request(req)
      .flatMap(_.body.asString)
      .flatMap(response => ZIO
        .fromEither(response.fromJson[T])
        .mapError(ParseError(_))
      )
  }

  def getJson[T](url: String)(using JsonDecoder[T]): ZIO[Any, Throwable, T] = {
    request(url)
      .flatMap(_.body.asString)
      .flatMap(response => ZIO
        .fromEither(response.fromJson[T])
        .mapError(ParseError(_))
      )
  }

  private def request(request: Request) = Client.request(request).provide(Client.default)

  private def request(url: String) = Client.request(url).provide(Client.default)
}

object HttpClient {

  val layer: URLayer[Client, HttpClient] = ZLayer {
    for {
      client <- ZIO.service[Client]
    } yield HttpClient()
  }
}