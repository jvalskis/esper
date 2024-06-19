package is.valsk.esper.api.ota

import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.domain.Types.NonEmptyStringExtractor
import is.valsk.esper.domain.Version
import zio.http.*
import zio.http.model.{HttpError, Method}
import zio.{URLayer, ZIO, ZLayer}

class OtaApi(
    getDeviceVersion: GetDeviceVersion,
    flashDevice: FlashDevice,
    getDeviceStatus: GetDeviceStatus,
    restartDevice: RestartDevice,
) {

  val app: HttpApp[Any, HttpError] = Http.collectZIO[Request] {
    case Method.GET -> !! / "ota" / NonEmptyStringExtractor(deviceId) / "version" => getDeviceVersion(deviceId)
    case Method.POST -> !! / "ota" / NonEmptyStringExtractor(deviceId) / "flash" => flashDevice(deviceId, None)
    case Method.POST -> !! / "ota" / NonEmptyStringExtractor(deviceId) / "flash" / Version(version) => flashDevice(deviceId, Some(version))
    case Method.POST -> !! / "ota" / NonEmptyStringExtractor(deviceId) / "status" => getDeviceStatus(deviceId)
    case Method.POST -> !! / "ota" / NonEmptyStringExtractor(deviceId) / "restart" => restartDevice(deviceId)
  }
}


object OtaApi {

  val layer: URLayer[RestartDevice & GetDeviceStatus & FlashDevice & GetDeviceVersion, OtaApi] = ZLayer.fromFunction(OtaApi(_, _, _, _))
}