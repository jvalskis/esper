package is.valsk.esper.api

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.api.devices.DeviceApi
import is.valsk.esper.api.ota.OtaApi
import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model, UrlString}
import is.valsk.esper.domain.{Device, Types, Version}
import is.valsk.esper.repositories.DeviceRepository
import zio.http.model.{HttpError, Method}
import zio.http.{Request, Response, URL}
import zio.json.*
import zio.{Exit, URIO, ZIO}

trait ApiSpec {

  protected val nonExistentDeviceId: DeviceId = NonEmptyString.unsafeFrom("non-existent-device-id")

  val manufacturer1: Manufacturer = Manufacturer.unsafeFrom("test-device-1")
  val manufacturerWithFailingHandler: Manufacturer = Manufacturer.unsafeFrom("failing-manufacturer")
  val unsupportedManufacturer: Manufacturer = Manufacturer.unsafeFrom("unsupported-manufacturer")
  protected val device1: Device = Device(
    id = NonEmptyString.unsafeFrom("id"),
    url = UrlString.unsafeFrom("https://fake.url"),
    name = NonEmptyString.unsafeFrom("name"),
    nameByUser = Some("nameByUser"),
    model = Model.unsafeFrom("model"),
    softwareVersion = Some(Version("softwareVersion")),
    manufacturer = manufacturer1,
  )

  def getDeviceVersion(deviceId: DeviceId): ZIO[OtaApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[OtaApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = getDeviceVersionEndpoint(deviceId))).exit
  } yield response

  def getDevice(deviceId: DeviceId): ZIO[DeviceApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[DeviceApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = getDeviceEndpoint(deviceId))).exit
  } yield response

  def listDevices: ZIO[DeviceApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[DeviceApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = getDevicesEndpoint)).exit
  } yield response

  def givenDevices(devices: Device*): URIO[DeviceRepository, Unit] = for {
    deviceRepository <- ZIO.service[DeviceRepository]
    _ <- ZIO.foreach(devices)(deviceRepository.add).orDie
  } yield ()

  def getDevicesEndpoint: URL = {
    URL.fromString("/devices").toOption.get
  }

  def otaEndpoint: URL = {
    URL.fromString("/ota").toOption.get
  }

  def getDeviceEndpoint(deviceId: DeviceId): URL = {
    getDevicesEndpoint ++ URL.fromString(s"/${deviceId.value}").toOption.get
  }

  def otaEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint ++ URL.fromString(s"/${deviceId.value}").toOption.get
  }

  def getDeviceVersionEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint(deviceId) ++ URL.fromString(s"/version").toOption.get
  }

  //  def parseResponse[T](response: Response)(using JsonDecoder[T]): ZIO[Any, Serializable, T] = {
  //    response.body.asString.flatMap(x => ZIO.fromEither(x.fromJson[T]))
  //  }

  def parseResponse[T](response: Exit[Option[HttpError], Response])(using JsonDecoder[T]): ZIO[Any, Any, T] = {
    response.map(_.body.asString.flatMap(x => ZIO.fromEither(x.fromJson[T]))).flatten
  }
}
