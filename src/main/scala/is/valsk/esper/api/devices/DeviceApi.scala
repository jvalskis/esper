package is.valsk.esper.api.devices

import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdates, ListDevices}
import is.valsk.esper.domain.Types.DeviceIdExtractor
import zio.http.*
import zio.http.model.{HttpError, Method}
import zio.{URLayer, ZIO, ZLayer}

class DeviceApi(
    getDevices: ListDevices,
    getDevice: GetDevice,
    getPendingUpdates: GetPendingUpdates,
) {

  val app: HttpApp[Any, HttpError] = Http.collectZIO[Request] {
    case Method.GET -> !! / "devices" => getDevices()
    case Method.GET -> !! / "devices" / "updates" => getPendingUpdates()
    case Method.GET -> !! / "devices" / DeviceIdExtractor(deviceId) => getDevice(deviceId)
  }
}

object DeviceApi {

  val layer: URLayer[ListDevices & GetDevice & GetPendingUpdates, DeviceApi] = ZLayer.fromFunction(DeviceApi(_, _, _))
}