package is.valsk.esper.api.devices

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.EsperConfig
import is.valsk.esper.api.devices.DeviceApi
import is.valsk.esper.api.devices.endpoints.{GetDevice, ListDevices}
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceVersion}
import is.valsk.esper.device.DeviceProxy
import is.valsk.esper.domain.Device.encoder
import is.valsk.esper.domain.Types.{DeviceIdExtractor, NonEmptyStringExtractor}
import is.valsk.esper.domain.Version.encoder
import is.valsk.esper.domain.{DeviceModel, SemanticVersion, Version}
import is.valsk.esper.repositories.{DeviceRepository, InMemoryDeviceRepository, Repository}
import is.valsk.esper.services.FirmwareDownloader
import zio.http.*
import zio.http.model.{HttpError, Method, Status}
import zio.http.netty.NettyServerConfig
import zio.json.*
import zio.{Random, Task, URLayer, ZIO, ZLayer}

class DeviceApi(
    getDevices: ListDevices,
    getDevice: GetDevice,
) {

  val app: HttpApp[Any, HttpError] = Http.collectZIO[Request] {
    case Method.GET -> !! / "devices" => getDevices()
    case Method.GET -> !! / "devices" / DeviceIdExtractor(deviceId) => getDevice(deviceId)
  }
}

object DeviceApi {

  val layer: URLayer[ListDevices & GetDevice, DeviceApi] = ZLayer.fromFunction(DeviceApi(_, _))
}