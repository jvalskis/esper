package is.valsk.esper.api.ota

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.device.*
import is.valsk.esper.device.DeviceStatus.UpdateStatus
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareService, OtaService}
import zio.*
import zio.http.Response
import zio.http.model.HttpError
import zio.test.*
import zio.test.Assertion.*

object FlashDeviceSpec extends ZIOSpecDefault with ApiSpec {

  def spec = suite("FlashDeviceSpec")(
    suite("without explicit version")(
      test("Return a 404 (Not Found) if the device does not exist") {
        for {
          _ <- givenDevices(device1)
          response <- flashDevice(nonExistentDeviceId)
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
          response <- flashDevice(device1.id)
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.InternalServerError("message"))))
        )
      }
        .provide(
          deviceRepositoryLayerThatThrowsException,
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
      test("Fail with 412 (Precondition Failed) when latest firmware is not found") {
        for {
          _ <- givenDevices(device1)
          response <- flashDevice(device1.id)
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.PreconditionFailed(s"Latest firmware not found"))))
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
      test("Fail with 412 (Precondition Failed) when latest firmware is not found (mismatching manufacturer)") {
        for {
          _ <- givenFirmware(Firmware(
            manufacturer = otherManufacturer,
            model = device1.model,
            version = Version("version"),
            data = Array.emptyByteArray,
            size = 0,
          ))
          _ <- givenDevices(device1)
          response <- flashDevice(device1.id)
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.PreconditionFailed(s"Latest firmware not found"))))
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
      test("Fail with 412 (Precondition Failed) when latest firmware is not found (mismatching model)") {
        for {
          _ <- givenFirmware(Firmware(
            manufacturer = device1.manufacturer,
            model = otherModel,
            version = Version("version"),
            data = Array.emptyByteArray,
            size = 0,
          ))
          _ <- givenDevices(device1)
          response <- flashDevice(device1.id)
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.PreconditionFailed(s"Latest firmware not found"))))
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
      test("Fail with 412 (Precondition Failed) when manufacturer is not supported") {
        for {
          _ <- givenDevices(device1.copy(manufacturer = unsupportedManufacturer))
          response <- flashDevice(device1.id)
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.PreconditionFailed(s"Manufacturer not supported: $unsupportedManufacturer"))))
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
      test("Fail with 502 (Bad Gateway) when there is an exception while calling the device") {
        for {
          _ <- givenFirmware(Firmware(
            manufacturer = manufacturerWithFailingHandler,
            model = device1.model,
            version = Version("version"),
            data = Array.emptyByteArray,
            size = 0,
          ))
          _ <- givenDevices(device1.copy(manufacturer = manufacturerWithFailingHandler))
          response <- flashDevice(device1.id)
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.BadGateway("error"))))
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
      test("Flash the device with the latest firmware") {
        for {
          _ <- givenFirmware(Firmware(
            manufacturer = device1.manufacturer,
            model = device1.model,
            version = Version("currentVersion"),
            data = Array.emptyByteArray,
            size = 0,
          ))
          _ <- givenDevices(device1)
          device <- flashDevice(device1.id)
            .flatMap(parseResponse[FlashResult])
        } yield {
          assert(device)(equalTo(FlashResult(
            previousVersion = Version("previousVersion"),
            currentVersion = Version("currentVersion"),
            updateStatus = UpdateStatus.done
          )))
        }
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
    )
  )

  private val manufacturerRegistryLayer: ULayer[Map[String, DeviceHandler]] = ZLayer {
    val value = for {
      testDeviceHandler <- ZIO.service[TestDeviceHandler]
      testFailingTestDeviceHandlerHandler <- ZIO.service[FailingTestDeviceHandler]
    } yield Map(
      testDeviceHandler.supportedManufacturer.toString -> testDeviceHandler,
      testFailingTestDeviceHandlerHandler.supportedManufacturer.toString -> testFailingTestDeviceHandlerHandler
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

    override def flashFirmware(device: Device, firmware: Firmware): IO[DeviceApiError, FlashResult] = ZIO.succeed(FlashResult(
      previousVersion = Version("previousVersion"),
      currentVersion = Version("currentVersion"),
      updateStatus = UpdateStatus.done
    ))

    override def getDeviceStatus(device: Device): IO[DeviceApiError, DeviceStatus] = ZIO.succeed(DeviceStatus(UpdateStatus.done))

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

    override def getDeviceStatus(device: Device): IO[DeviceApiError, DeviceStatus] = ZIO.fail(ApiCallFailed("error", device))

    override def restartDevice(device: Device): IO[DeviceApiError, Unit] = ???

    override def parseVersion(version: String): Either[MalformedVersion, Version] = ???

    def supportedManufacturer: Manufacturer = manufacturerWithFailingHandler
  }
}