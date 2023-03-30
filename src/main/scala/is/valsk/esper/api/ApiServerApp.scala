package is.valsk.esper.api

import is.valsk.esper.EsperConfig
import zio.http.model.Method
import zio.http.netty.NettyServerConfig
import zio.http.*
import zio.{Random, ZIO}

object ApiServerApp {

  private val app: HttpApp[Any, Nothing] = Http.collect[Request] {
    case Method.GET -> !! / "text" => Response.text("Hello World!")
  }

  private val zApp: UHttpApp = Http.collectZIO[Request] {
    case Method.POST -> !! / "text" => Random.nextIntBetween(3, 5).map(n => Response.text("Hello" * n + " World!"))
  }

  def apply(): ZIO[EsperConfig, Throwable, Nothing] = for {
    port <- EsperConfig.port
    serverConfigLayer = ServerConfig.live(
      ServerConfig.default.port(port)
    )
    httpServer = Server.install(app ++ zApp).flatMap { port =>
      ZIO.logInfo(s"Starting server on http://localhost:$port")
    }
    server <- (httpServer *> ZIO.never).provide(
      serverConfigLayer,
      NettyServerConfig.live,
      Server.customized
    )
  } yield server
}
