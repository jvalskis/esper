package is.valsk.esper.api

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.EsperConfig
import is.valsk.esper.api.DeviceApi
import is.valsk.esper.api.devices.{GetDevice, GetDeviceVersion, GetDevices}
import is.valsk.esper.device.DeviceProxy
import is.valsk.esper.device.shelly.ShellyDeviceHandler.ShellyDevice
import is.valsk.esper.domain.Device.encoder
import is.valsk.esper.domain.SemanticVersion.encoder
import is.valsk.esper.domain.Types.NonEmptyStringExtractor
import is.valsk.esper.domain.{DeviceModel, SemanticVersion}
import is.valsk.esper.repositories.{DeviceRepository, InMemoryDeviceRepository, Repository}
import is.valsk.esper.services.FirmwareDownloader
import zio.http.*
import zio.http.model.{HttpError, Method, Status}
import zio.http.netty.NettyServerConfig
import zio.json.*
import zio.{Random, Task, URLayer, ZIO, ZLayer}

class DeviceApi(
    getDevices: GetDevices,
    getDevice: GetDevice,
    getDeviceVersion: GetDeviceVersion,
) {

  val аpp: HttpApp[Any, HttpError] = Http.collectZIO[Request] {
    case Method.GET -> !! / "devices" => getDevices()
    case Method.GET -> !! / "devices" / NonEmptyStringExtractor(deviceId) => getDevice(deviceId)
    case Method.GET -> !! / "devices" / NonEmptyStringExtractor(deviceId) / "version" => getDeviceVersion(deviceId)
  }
}

object DeviceApi {

  val layer: URLayer[GetDevices & GetDevice & GetDeviceVersion, DeviceApi] = ZLayer {
    for {
      getDevices <- ZIO.service[GetDevices]
      getDevice <- ZIO.service[GetDevice]
      getDeviceVersion <- ZIO.service[GetDeviceVersion]
    } yield DeviceApi(getDevices, getDevice, getDeviceVersion)
  }
}