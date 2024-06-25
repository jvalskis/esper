package is.valsk.esper.services

import is.valsk.esper.domain.Types.UrlString
import sttp.client3.httpclient.zio.{HttpClientZioBackend, SttpClient}
import sttp.client3.{Response, ResponseAs, UriContext, asByteArrayAlways, asString, basicRequest}
import sttp.model.Uri
import zio.json.*
import zio.stream.*
import zio.{Task, TaskLayer, ZIO, ZLayer}

class HttpClient(client: SttpClient) {

  def get(url: String): Task[Response[Either[String, String]]] = basicRequest
    .get(uri"$url")
    .send(client)

  def download(url: String): Stream[Throwable, Byte] = ZStream.fromIteratorZIO(for {
    res <- basicRequest
      .get(uri"$url")
      .response(asByteArrayAlways)
      .send(client)
  } yield res.body.iterator)

  def getJson[T](url: String)(using JsonDecoder[T]): Task[T] = for {
    res <- basicRequest
      .get(uri"$url")
      .response(HttpClient.parseJson[T])
      .send(client)
    result <- ZIO.fromEither(res.body)
      .mapError(RuntimeException(_))
  } yield result

}

object HttpClient {

  private def parseJson[T](using JsonDecoder[T]): ResponseAs[Either[String, T], Any] = asString.map(_.flatMap(_.fromJson[T]))

  val layer: TaskLayer[HttpClient] = ZLayer {
    for {
      client <- HttpClientZioBackend()
    } yield HttpClient(client)
  }

  val configuredLayer: TaskLayer[HttpClient] = HttpClientZioBackend.layer() >>> layer

  extension (urlString: UrlString)
    def asUri: Uri = uri"${urlString.toString}"
}