package is.valsk.esper.api.devices

import is.valsk.esper.api.BaseController
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.http.endpoints.DeviceEndpoints
import sttp.tapir.*
import sttp.tapir.server.ServerEndpoint
import zio.{Task, URLayer, ZLayer}

class DeviceApi(
    listDevices: ListDevices,
    getDevice: GetDevice,
    getPendingUpdates: GetPendingUpdates,
    getPendingUpdate: GetPendingUpdate,
) extends DeviceEndpoints with BaseController {

  val listDevicesEndpointImpl: ServerEndpoint[Any, Task] = listDevicesEndpoint.serverLogic(_ => listDevices().either)
  val getDeviceEndpointImpl: ServerEndpoint[Any, Task] = getDeviceEndpoint.serverLogic { case DeviceId(deviceId) => getDevice(deviceId).either }
  val getPendingUpdateEndpointImpl: ServerEndpoint[Any, Task] = getPendingUpdateEndpoint.serverLogic { case DeviceId(deviceId) => getPendingUpdate(deviceId).either }
  val getPendingUpdatesEndpointImpl: ServerEndpoint[Any, Task] = getPendingUpdatesEndpoint.serverLogic(_ => getPendingUpdates().either)

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    getDeviceEndpointImpl,
    listDevicesEndpointImpl,
    getPendingUpdateEndpointImpl,
    getPendingUpdatesEndpointImpl,
  )
}

object DeviceApi {

  val layer: URLayer[ListDevices & GetDevice & GetPendingUpdates & GetPendingUpdate, DeviceApi] = ZLayer.fromFunction(DeviceApi(_, _, _, _))
}