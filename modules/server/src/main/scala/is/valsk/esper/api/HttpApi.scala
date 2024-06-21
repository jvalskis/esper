package is.valsk.esper.api

import is.valsk.esper.api.devices.DeviceApi
import is.valsk.esper.api.firmware.FirmwareApi
import is.valsk.esper.api.ota.OtaApi
import sttp.tapir.server.ServerEndpoint
import zio.{Task, URLayer, ZIO, ZLayer}

trait HttpApi {
  def endpointsZIO: Task[List[ServerEndpoint[Any, Task]]]
}

object HttpApi {
  private class HttpApiLive(
      firmwareApi: FirmwareApi,
      otaApi: OtaApi,
      deviceApi: DeviceApi,
  ) extends HttpApi {

    def endpointsZIO: Task[List[ServerEndpoint[Any, Task]]] = ZIO.succeed(gatherRoutes(List(firmwareApi, otaApi, deviceApi)))

    private def gatherRoutes(controllers: List[BaseController]): List[ServerEndpoint[Any, Task]] = controllers.flatMap(_.routes)
  }

  val layer: URLayer[OtaApi & DeviceApi & FirmwareApi, HttpApi] = ZLayer.fromFunction(HttpApiLive(_, _, _))
}