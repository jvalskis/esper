package is.valsk.esper.api.ota

import is.valsk.esper.api.BaseController
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.domain.Types.DeviceIdExtractor
import is.valsk.esper.domain.Version
import is.valsk.esper.http.endpoints.OtaEndpoints
import sttp.tapir.server.ServerEndpoint
import zio.{Task, URLayer, ZLayer}

class OtaApi(
    getDeviceVersion: GetDeviceVersion,
    flashDevice: FlashDevice,
    getDeviceStatus: GetDeviceStatus,
    restartDevice: RestartDevice,
) extends OtaEndpoints with BaseController {

  val getDeviceVersionEndpointImpl: ServerEndpoint[Any, Task] = getDeviceVersionEndpoint.serverLogic { case DeviceIdExtractor(deviceId) =>
    getDeviceVersion(deviceId).either
  }
  val flashDeviceEndpointImpl: ServerEndpoint[Any, Task] = flashDeviceEndpoint.serverLogic { case (DeviceIdExtractor(deviceId), Version(version)) =>
    flashDevice(deviceId, Some(version)).either
  }
  val flashDeviceWithLatestVersionEndpointImpl: ServerEndpoint[Any, Task] = flashDeviceWithLatestVersionEndpoint.serverLogic { case DeviceIdExtractor(deviceId) =>
    flashDevice(deviceId, None).either
  }
  val getDeviceStatusEndpointImpl: ServerEndpoint[Any, Task] = getDeviceStatusEndpoint.serverLogic { case DeviceIdExtractor(deviceId) =>
    getDeviceStatus(deviceId).either
  }
  val restartDeviceEndpointImpl: ServerEndpoint[Any, Task] = restartDeviceEndpoint.serverLogic { case DeviceIdExtractor(deviceId) =>
    restartDevice(deviceId).either
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    getDeviceVersionEndpointImpl,
    flashDeviceEndpointImpl,
    flashDeviceWithLatestVersionEndpointImpl,
    getDeviceStatusEndpointImpl,
    restartDeviceEndpointImpl,
  )
}

object OtaApi {

  val layer: URLayer[RestartDevice & GetDeviceStatus & FlashDevice & GetDeviceVersion, OtaApi] = ZLayer.fromFunction(OtaApi(_, _, _, _))
}