package is.valsk.esper.api.devices

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.DeviceId
import zio.{Exit, ZIO}
import zio.http.{Request, Response, URL}
import zio.http.model.{HttpError, Method}

trait DevicesSpec extends ApiSpec {

  def getDevice(deviceId: DeviceId): ZIO[DeviceApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[DeviceApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = getDeviceEndpoint(deviceId))).exit
  } yield response

  def getPendingUpdate(deviceId: DeviceId): ZIO[DeviceApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[DeviceApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = getPendingUpdateEndpoint(deviceId))).exit
  } yield response

  def getPendingUpdates: ZIO[DeviceApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[DeviceApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = getPendingUpdatesEndpoint)).exit
  } yield response

  def listDevices: ZIO[DeviceApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[DeviceApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = getDevicesEndpoint)).exit
  } yield response

  def getDevicesEndpoint: URL = {
    URL.fromString("/devices").toOption.get
  }

  def getDeviceEndpoint(deviceId: DeviceId): URL = {
    getDevicesEndpoint ++ URL.fromString(s"/$deviceId").toOption.get
  }

  def getPendingUpdateEndpoint(deviceId: DeviceId): URL = {
    getDevicesEndpoint ++ URL.fromString(s"updates/$deviceId").toOption.get
  }

  def getPendingUpdatesEndpoint: URL = {
    getDevicesEndpoint ++ URL.fromString(s"updates").toOption.get
  }
}
