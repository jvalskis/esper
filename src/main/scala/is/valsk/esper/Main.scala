package is.valsk.esper

import zio.*
import zio.http.*
import zio.http.model.Method
import zio.http.netty.NettyServerConfig

object Main extends ZIOAppDefault {

  private val port = 9000

  val app: HttpApp[Any, Nothing] = Http.collect[Request] {
    case Method.GET -> !! / "text" => Response.text("Hello World!")
  }

  val zApp: UHttpApp = Http.collectZIO[Request] {
    case Method.POST -> !! / "text" => Random.nextIntBetween(3, 5).map(n => Response.text("Hello" * n + " World!"))
  }

  override val run: ZIO[Environment with ZIOAppArgs with Scope, Any, Any] = ZIOAppArgs.getArgs.flatMap { args =>
    val config = ServerConfig.default
      .port(port)
    val configLayer = ServerConfig.live(config)
    val nettyConfigLayer = NettyServerConfig.live(NettyServerConfig.default)
    val server = Server.install(app ++ zApp).flatMap { port =>
      Console.printLine(s"Starting server on http://localhost:$port")
    }
    (server *> ZIO.never).provide(configLayer, nettyConfigLayer, Server.customized)
  }

}