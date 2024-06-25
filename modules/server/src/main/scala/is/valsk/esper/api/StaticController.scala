package is.valsk.esper.api

import sttp.tapir.files.{Resources, staticResourcesGetEndpoint}
import sttp.tapir.server.ServerEndpoint
import zio.{Task, ULayer, ZIO, ZLayer}

class StaticController extends BaseController {

  override val routes: List[ServerEndpoint[Any, Task]] = ServerEndpoint.public(
    staticResourcesGetEndpoint,
    Resources.get[Task](getClass.getClassLoader, "app")
  ) :: Nil
}

object StaticController {
  val layer: ULayer[StaticController] = ZLayer.succeed(new StaticController)
}