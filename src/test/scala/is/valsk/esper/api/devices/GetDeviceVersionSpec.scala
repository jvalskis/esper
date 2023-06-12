package is.valsk.esper.api.devices

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.EsperConfig
import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.ApiSpec
import is.valsk.esper.api.devices.endpoints.{GetDevice, ListDevices}
import is.valsk.esper.api.firmware.FirmwareApi
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceVersion}
import is.valsk.esper.device.shelly.{ShellyConfig, ShellyDeviceHandler}
import is.valsk.esper.device.{DeviceManufacturerHandler, DeviceProxy, DeviceProxyRegistry}
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
import is.valsk.esper.domain.*
import is.valsk.esper.hass.HassToDomainMapper
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.repositories.{DeviceRepository, InMemoryDeviceRepository, InMemoryFirmwareRepository}
import is.valsk.esper.services.HttpClient
import zio.*
import zio.http.model.{HttpError, Method, Status}
import zio.http.{Client, Request, Response, URL}
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
        layerWithTestRepository,
        InMemoryFirmwareRepository.layer,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetDeviceVersion.layer,
        FlashDevice.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
        ShellyDeviceHandler.layer,
        HttpClient.layer,
        EsperConfig.layer,
        ShellyConfig.layer,
        Client.default,
      ),
    test("Fail with 500 (Internal Server Error) when there is an exception while fetching the device") {
      for {
        _ <- givenDevices(device1)
        response <- getDeviceVersion(device1.id)
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.InternalServerError())))
      )
    }
      .provide(layerThatThrowsException,
        InMemoryFirmwareRepository.layer,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetDeviceVersion.layer,
        FlashDevice.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
        ShellyDeviceHandler.layer,
        HttpClient.layer,
        EsperConfig.layer,
        ShellyConfig.layer,
        Client.default,
      ),
    test("Fail with 412 (Precondition Failed) when device manufacturer is not supported") {
      for {
        _ <- givenDevices(device1.copy(manufacturer = unsupportedManufacturer))
        response <- getDeviceVersion(device1.id)
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.PreconditionFailed(s"Manufacturer not supported: $unsupportedManufacturer"))))
      )
    }
      .provide(layerWithTestRepository,
        InMemoryFirmwareRepository.layer,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetDeviceVersion.layer,
        FlashDevice.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
        ShellyDeviceHandler.layer,
        HttpClient.layer,
        EsperConfig.layer,
        ShellyConfig.layer,
        Client.default,
      ),
    test("Fail with 502 (Bad Gateway) when there is an exception while calling the device") {
      for {
        _ <- givenDevices(device1.copy(manufacturer = manufacturerWithFailingHandler))
        response <- getDeviceVersion(device1.id)
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.BadGateway("error"))))
      )
    }
      .provide(layerWithTestRepository,
        InMemoryFirmwareRepository.layer,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetDeviceVersion.layer,
        FlashDevice.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
        ShellyDeviceHandler.layer,
        HttpClient.layer,
        EsperConfig.layer,
        ShellyConfig.layer,
        Client.default,
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
      .provide(layerWithTestRepository,
        InMemoryFirmwareRepository.layer,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetDeviceVersion.layer,
        FlashDevice.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
        ShellyDeviceHandler.layer,
        HttpClient.layer,
        EsperConfig.layer,
        ShellyConfig.layer,
        Client.default,
      ),
  )

  val layerWithTestRepository: ULayer[DeviceRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[NonEmptyString, Device])
    } yield InMemoryDeviceRepository(ref)
  }

  val layerThatThrowsException: ULayer[DeviceRepository] = ZLayer.succeed(
    new DeviceRepository {
      override def get(id: NonEmptyString): IO[PersistenceException, Option[Device]] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))

      override def getAll: IO[PersistenceException, List[Device]] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))

      override def add(device: Device): IO[PersistenceException, Device] = ZIO.succeed(device)
    }
  )

  private val manufacturerRegistryLayer: URLayer[Any, Map[Manufacturer, DeviceManufacturerHandler with HassToDomainMapper with DeviceProxy]] = ZLayer {
    val value = for {
      testDeviceHandler <- ZIO.service[TestDeviceHandler]
      testfailingTestDeviceHandlerHandler <- ZIO.service[FailingTestDeviceHandler]
    } yield Map(
      testDeviceHandler.supportedManufacturer -> testDeviceHandler,
      testfailingTestDeviceHandlerHandler.supportedManufacturer -> testfailingTestDeviceHandlerHandler
    )
    value.provide(
      ZLayer.succeed(TestDeviceHandler()),
      ZLayer.succeed(FailingTestDeviceHandler())
    )
  }

  case class TestDeviceHandler() extends DeviceManufacturerHandler with HassToDomainMapper with DeviceProxy {
    override def getFirmwareDownloadDetails(
        manufacturer: Manufacturer,
        model: Model,
        version: Option[Version]
    ): IO[FirmwareDownloadError, DeviceManufacturerHandler.FirmwareDescriptor] = ???

    override def versionOrdering: Ordering[Version] = Ordering.String.on(_.value)

    override def toDomain(hassDevice: HassResult): IO[String, Device] = ???

    override def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, Version] = ZIO.succeed(Version("currentFirmwareVersion"))

    override def flashFirmware(device: Device, firmware: Firmware): IO[Throwable, Unit] = ???

    def supportedManufacturer: Manufacturer = manufacturer1
  }

  case class FailingTestDeviceHandler() extends DeviceManufacturerHandler with HassToDomainMapper with DeviceProxy {
    override def getFirmwareDownloadDetails(
        manufacturer: Manufacturer,
        model: Model,
        version: Option[Version]
    ): IO[FirmwareDownloadError, DeviceManufacturerHandler.FirmwareDescriptor] = ???

    override def versionOrdering: Ordering[Version] = Ordering.String.on(_.value)

    override def toDomain(hassDevice: HassResult): IO[String, Device] = ???

    override def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, Version] = ZIO.fail(ApiCallFailed("error", device))

    override def flashFirmware(device: Device, firmware: Firmware): IO[Throwable, Unit] = ZIO.fail(ApiCallFailed("error", device))

    def supportedManufacturer: Manufacturer = manufacturerWithFailingHandler
  }
}