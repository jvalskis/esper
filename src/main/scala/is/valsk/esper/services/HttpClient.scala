package is.valsk.esper.services

import zio.http.{Client, Response}
import zio.{URLayer, ZIO, ZLayer}

class HttpClient {

  def get(url: String): ZIO[Any, Throwable, Response] = {
    Client.request(url)
      .provide(Client.default)
  }
}

object HttpClient {

  val layer: URLayer[Client, HttpClient] = ZLayer {
    for {
      client <- ZIO.service[Client]
    } yield HttpClient()
  }
}