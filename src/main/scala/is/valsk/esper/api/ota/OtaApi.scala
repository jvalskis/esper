package is.valsk.esper.api.ota

import is.valsk.esper.api.devices.DeviceApi
import is.valsk.esper.api.devices.endpoints.{GetDevice, ListDevices}
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceVersion}
import is.valsk.esper.domain.Types.NonEmptyStringExtractor
import is.valsk.esper.domain.Version
import zio.http.model.{HttpError, Method, Status}
import zio.http.*
import zio.{URLayer, ZIO, ZLayer}

class OtaApi(
    getDeviceVersion: GetDeviceVersion,
    flashDevice: FlashDevice,
) {

  val app: HttpApp[Any, HttpError] = Http.collectZIO[Request] {
    case Method.GET -> !! / "ota" / NonEmptyStringExtractor(deviceId) / "version" => getDeviceVersion(deviceId)
    case Method.POST -> !! / "ota" / NonEmptyStringExtractor(deviceId) / "flash" => flashDevice(deviceId, None)
    case Method.POST -> !! / "ota" / NonEmptyStringExtractor(deviceId) / "flash" / Version(version) => flashDevice(deviceId, Some(version))
  }
}


object OtaApi {

  val layer: URLayer[FlashDevice & GetDeviceVersion, OtaApi] = ZLayer.fromFunction(OtaApi(_, _))
}