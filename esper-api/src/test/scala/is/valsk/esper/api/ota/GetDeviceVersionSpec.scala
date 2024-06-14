package is.valsk.esper.api.ota

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.api.ApiSpec
import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.device.{DeviceHandler, DeviceManufacturerHandler, DeviceProxyRegistry, DeviceStatus, FlashResult}
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.repositories.{DeviceRepository, InMemoryDeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareService, OtaService}
import zio.*
import zio.http.Response
import zio.http.model.HttpError
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

import java.io.IOException


object GetDeviceVersionSpec extends ZIOSpecDefault with ApiSpec {

  def spec = suite("GetDeviceVersionSpec")(
    test("Return a 404 (Not Found) if the device does not exist") {
      for {
        _ <- givenDevices(device1)
        response <- getDeviceVersion(nonExistentDeviceId)
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.NotFound(""))))
      )
    }
      .provide(
        deviceRepositoryLayerWithTestRepository,
        InMemoryManufacturerRepository.layer,
        InMemoryFirmwareRepository.layer,
        FirmwareService.layer,
        RestartDevice.layer,
        GetDeviceStatus.layer,
        FlashDevice.layer,
        GetDeviceVersion.layer,
        OtaService.layer,
        OtaApi.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
      ),
    test("Fail with 500 (Internal Server Error) when there is an exception while fetching the device") {
      for {
        _ <- givenDevices(device1)
        response <- getDeviceVersion(device1.id)
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.InternalServerError("message"))))
      )
    }
      .provide(deviceRepositoryLayerThatThrowsException,
        InMemoryManufacturerRepository.layer,
        InMemoryFirmwareRepository.layer,
        FirmwareService.layer,
        RestartDevice.layer,
        GetDeviceStatus.layer,
        FlashDevice.layer,
        GetDeviceVersion.layer,
        OtaService.layer,
        OtaApi.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
      ),
    test("Fail with 412 (Precondition Failed) when device manufacturer is not supported") {
      for {
        _ <- givenDevices(device1.copy(manufacturer = unsupportedManufacturer))
        response <- getDeviceVersion(device1.id)
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.PreconditionFailed(s"Manufacturer not supported: $unsupportedManufacturer"))))
      )
    }
      .provide(deviceRepositoryLayerWithTestRepository,
        InMemoryManufacturerRepository.layer,
        InMemoryFirmwareRepository.layer,
        FirmwareService.layer,
        RestartDevice.layer,
        GetDeviceStatus.layer,
        FlashDevice.layer,
        GetDeviceVersion.layer,
        OtaService.layer,
        OtaApi.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
      ),
    test("Fail with 502 (Bad Gateway) when there is an exception while calling the device") {
      for {
        _ <- givenDevices(device1.copy(manufacturer = manufacturerWithFailingHandler))
        response <- getDeviceVersion(device1.id)
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.BadGateway("error"))))
      )
    }
      .provide(deviceRepositoryLayerWithTestRepository,
        InMemoryManufacturerRepository.layer,
        InMemoryFirmwareRepository.layer,
        FirmwareService.layer,
        RestartDevice.layer,
        GetDeviceStatus.layer,
        FlashDevice.layer,
        GetDeviceVersion.layer,
        OtaService.layer,
        OtaApi.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
      ),
    test("Return the device version") {
      for {
        _ <- givenDevices(device1)
        device <- getDeviceVersion(device1.id)
          .flatMap(parseResponse[String])
      } yield {
        assert(device)(equalTo("currentFirmwareVersion"))
      }
    }
      .provide(deviceRepositoryLayerWithTestRepository,
        InMemoryManufacturerRepository.layer,
        InMemoryFirmwareRepository.layer,
        FirmwareService.layer,
        RestartDevice.layer,
        GetDeviceStatus.layer,
        FlashDevice.layer,
        GetDeviceVersion.layer,
        OtaService.layer,
        OtaApi.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
      ),
  )

  val deviceRepositoryLayerWithTestRepository: ULayer[DeviceRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[DeviceId, Device])
    } yield InMemoryDeviceRepository(ref)
  }

  val deviceRepositoryLayerThatThrowsException: ULayer[DeviceRepository] = ZLayer.succeed(
    new DeviceRepository {
      override def get(id: DeviceId): IO[PersistenceException, Device] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))

      override def getOpt(id: DeviceId): IO[PersistenceException, Option[Device]] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))

      override def getAll: IO[PersistenceException, List[Device]] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))

      override def add(device: Device): IO[PersistenceException, Device] = ZIO.succeed(device)

      override def update(device: Device): IO[PersistenceException, Device] = ZIO.succeed(device)
    }
  )

  private val manufacturerRegistryLayer: URLayer[Any, Map[Manufacturer, DeviceHandler]] = ZLayer {
    val value = for {
      testDeviceHandler <- ZIO.service[TestDeviceHandler]
      testFailingTestDeviceHandlerHandler <- ZIO.service[FailingTestDeviceHandler]
    } yield Map(
      testDeviceHandler.supportedManufacturer -> testDeviceHandler,
      testFailingTestDeviceHandlerHandler.supportedManufacturer -> testFailingTestDeviceHandlerHandler
    )
    value.provide(
      ZLayer.succeed(TestDeviceHandler()),
      ZLayer.succeed(FailingTestDeviceHandler())
    )
  }

  case class TestDeviceHandler() extends DeviceHandler {
    override def getFirmwareDownloadDetails(
        manufacturer: Manufacturer,
        model: Model,
        version: Option[Version]
    ): IO[FirmwareDownloadError, DeviceManufacturerHandler.FirmwareDescriptor] = ???

    override def versionOrdering: Ordering[Version] = Ordering.String.on(_.value)

    override def toDomain(hassDevice: HassResult): IO[MalformedVersion | ParseError, Device] = ???

    override def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, Version] = ZIO.succeed(Version("currentFirmwareVersion"))

    override def flashFirmware(device: Device, firmware: Firmware): IO[DeviceApiError, FlashResult] = ???

    override def getDeviceStatus(device: Device): IO[DeviceApiError, DeviceStatus] = ???

    override def restartDevice(device: Device): IO[DeviceApiError, Unit] = ???

    override def parseVersion(version: String): Either[MalformedVersion, Version] = ???

    def supportedManufacturer: Manufacturer = manufacturer1
  }

  case class FailingTestDeviceHandler() extends DeviceHandler {
    override def getFirmwareDownloadDetails(
        manufacturer: Manufacturer,
        model: Model,
        version: Option[Version]
    ): IO[FirmwareDownloadError, DeviceManufacturerHandler.FirmwareDescriptor] = ???

    override def versionOrdering: Ordering[Version] = Ordering.String.on(_.value)

    override def toDomain(hassDevice: HassResult): IO[MalformedVersion | ParseError, Device] = ???

    override def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, Version] = ZIO.fail(ApiCallFailed("error", device))

    override def flashFirmware(device: Device, firmware: Firmware): IO[DeviceApiError, FlashResult] = ZIO.fail(ApiCallFailed("error", device))

    override def getDeviceStatus(device: Device): IO[DeviceApiError, DeviceStatus] = ???

    override def restartDevice(device: Device): IO[DeviceApiError, Unit] = ???

    override def parseVersion(version: String): Either[MalformedVersion, Version] = ???

    def supportedManufacturer: Manufacturer = manufacturerWithFailingHandler
  }
}