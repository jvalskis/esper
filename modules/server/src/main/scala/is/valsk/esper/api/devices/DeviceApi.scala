package is.valsk.esper.api.devices

import is.valsk.esper.api.BaseController
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.domain.Types.DeviceIdExtractor
import is.valsk.esper.http.endpoints.DeviceEndpoints
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import zio.{Task, URLayer, ZIO, ZLayer}

class DeviceApi(
    getDevices: ListDevices,
    getDevice: GetDevice,
    getPendingUpdates: GetPendingUpdates,
    getPendingUpdate: GetPendingUpdate,
) extends DeviceEndpoints with BaseController {

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    listDevicesEndpoint
      .serverLogic(_ => getDevices().either),
    getDeviceEndpoint
      .serverLogic { case DeviceIdExtractor(deviceId) => getDevice(deviceId).either },
    getPendingUpdateEndpoint
      .serverLogic { case DeviceIdExtractor(deviceId) => getPendingUpdate(deviceId).either },
    getPendingUpdatesEndpoint
      .serverLogic(_ => getPendingUpdates().either),
  )
}

object DeviceApi {

  val layer: URLayer[ListDevices & GetDevice & GetPendingUpdates & GetPendingUpdate, DeviceApi] = ZLayer.fromFunction(DeviceApi(_, _, _, _))
}