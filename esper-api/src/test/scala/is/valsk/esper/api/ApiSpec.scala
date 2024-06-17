package is.valsk.esper.api

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.api.devices.DeviceApi
import is.valsk.esper.api.ota.OtaApi
import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model, UrlString}
import is.valsk.esper.domain.{Device, Firmware, PersistenceException, Types, Version}
import is.valsk.esper.model.api.PendingUpdate
import is.valsk.esper.repositories.{DeviceRepository, FirmwareRepository, InMemoryDeviceRepository, InMemoryPendingUpdateRepository, PendingUpdateRepository}
import zio.http.model.{HttpError, Method}
import zio.http.{Request, Response, URL}
import zio.json.*
import zio.{Exit, IO, Ref, ULayer, URIO, ZIO, ZLayer}

import java.io.IOException

trait ApiSpec {

  protected val nonExistentDeviceId: DeviceId = NonEmptyString.unsafeFrom("non-existent-device-id")

  val manufacturer1: Manufacturer = Manufacturer.unsafeFrom("test-device-1")
  val otherManufacturer: Manufacturer = Manufacturer.unsafeFrom("otherManufacturer")
  val manufacturerWithFailingHandler: Manufacturer = Manufacturer.unsafeFrom("failing-manufacturer")
  val unsupportedManufacturer: Manufacturer = Manufacturer.unsafeFrom("unsupported-manufacturer")
  val model1: NonEmptyString = Model.unsafeFrom("model1")
  val otherModel: NonEmptyString = Model.unsafeFrom("other-model")
  protected val device1: Device = Device(
    id = NonEmptyString.unsafeFrom("id"),
    url = UrlString.unsafeFrom("https://fake.url"),
    name = NonEmptyString.unsafeFrom("name"),
    nameByUser = Some("nameByUser"),
    model = model1,
    softwareVersion = Some(Version("softwareVersion")),
    manufacturer = manufacturer1,
  )
  protected val pendingUpdate1: PendingUpdate = PendingUpdate(
    device = device1,
    version = Version("version"),
  )

  val deviceRepositoryLayerWithTestRepository: ULayer[DeviceRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[DeviceId, Device])
    } yield InMemoryDeviceRepository(ref)
  }

  val pendingUpdateRepositoryLayerWithTestRepository: ULayer[PendingUpdateRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[DeviceId, PendingUpdate])
    } yield InMemoryPendingUpdateRepository(ref)
  }

  val deviceRepositoryLayerThatThrowsException: ULayer[DeviceRepository] = ZLayer.succeed(
    new DeviceRepository {
      override def get(id: DeviceId): IO[PersistenceException, Device] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def getOpt(id: DeviceId): IO[PersistenceException, Option[Device]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def getAll: IO[PersistenceException, List[Device]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def add(device: Device): IO[PersistenceException, Device] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def update(device: Device): IO[PersistenceException, Device] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))
    }
  )

  val pendingUpdateRepositoryLayerThatThrowsException: ULayer[PendingUpdateRepository] = ZLayer.succeed(
    new PendingUpdateRepository {
      override def get(id: DeviceId): IO[PersistenceException, PendingUpdate] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def getOpt(id: DeviceId): IO[PersistenceException, Option[PendingUpdate]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def getAll: IO[PersistenceException, List[PendingUpdate]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def add(device: PendingUpdate): IO[PersistenceException, PendingUpdate] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def update(device: PendingUpdate): IO[PersistenceException, PendingUpdate] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))
    }
  )

  def getDeviceVersion(deviceId: DeviceId): ZIO[OtaApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[OtaApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = getDeviceVersionEndpoint(deviceId))).exit
  } yield response

  def getDeviceStatus(deviceId: DeviceId): ZIO[OtaApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[OtaApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.POST, url = getDeviceStatusEndpoint(deviceId))).exit
  } yield response

  def flashDevice(deviceId: DeviceId): ZIO[OtaApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[OtaApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.POST, url = flashDeviceStatusEndpoint(deviceId))).exit
  } yield response

  def flashDevice(deviceId: DeviceId, version: Version): ZIO[OtaApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[OtaApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.POST, url = flashDeviceStatusEndpoint(deviceId, version))).exit
  } yield response

  def restartDevice(deviceId: DeviceId): ZIO[OtaApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[OtaApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.POST, url = restartDeviceStatusEndpoint(deviceId))).exit
  } yield response

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

  def givenFirmwares(firmwares: Firmware*): URIO[FirmwareRepository, Unit] = for {
    firmwareRepository <- ZIO.service[FirmwareRepository]
    _ <- ZIO.foreach(firmwares)(firmwareRepository.add).orDie
  } yield ()

  def givenDevices(devices: Device*): URIO[DeviceRepository, Unit] = for {
    deviceRepository <- ZIO.service[DeviceRepository]
    _ <- ZIO.foreach(devices)(deviceRepository.add).orDie
  } yield ()

  def givenPendingUpdates(pendingUpdates: PendingUpdate*): URIO[PendingUpdateRepository, Unit] = for {
    pendingUpdateRepository <- ZIO.service[PendingUpdateRepository]
    _ <- ZIO.foreach(pendingUpdates)(pendingUpdateRepository.add).orDie
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

  def getPendingUpdateEndpoint(deviceId: DeviceId): URL = {
    getDevicesEndpoint ++ URL.fromString(s"updates/${deviceId.value}").toOption.get
  }

  def getPendingUpdatesEndpoint: URL = {
    getDevicesEndpoint ++ URL.fromString(s"updates").toOption.get
  }

  def otaEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint ++ URL.fromString(s"/${deviceId.value}").toOption.get
  }

  def getDeviceVersionEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint(deviceId) ++ URL.fromString(s"/version").toOption.get
  }

  def getDeviceStatusEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint(deviceId) ++ URL.fromString(s"/status").toOption.get
  }

  def flashDeviceStatusEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint(deviceId) ++ URL.fromString(s"/flash").toOption.get
  }

  def flashDeviceStatusEndpoint(deviceId: DeviceId, version: Version): URL = {
    flashDeviceStatusEndpoint(deviceId) ++ URL.fromString(s"/${version.value}").toOption.get
  }

  def restartDeviceStatusEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint(deviceId) ++ URL.fromString(s"/restart").toOption.get
  }

  def parseResponse[T](response: Exit[Option[HttpError], Response])(using JsonDecoder[T]): ZIO[Any, Any, T] = {
    response.map(_.body.asString.flatMap(x => ZIO.fromEither(x.fromJson[T]))).flatten
  }
}
