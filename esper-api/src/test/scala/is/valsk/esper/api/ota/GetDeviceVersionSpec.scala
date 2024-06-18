package is.valsk.esper.api.ota

import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.device.{DeviceHandler, DeviceProxyRegistry}
import is.valsk.esper.domain.*
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareDownloader, FirmwareService, OtaService, PendingUpdateService}
import zio.*
import zio.http.Response
import zio.http.model.HttpError
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object GetDeviceVersionSpec extends ZIOSpecDefault with OtaSpec {

  def spec = suite("GetDeviceVersionSpec")(
    test("Return a 404 (Not Found) if the device does not exist") {
      val mockEmailService = MockEmailService.empty
      {
        for {
          _ <- givenDevices(device1)
          response <- getDeviceVersion(nonExistentDeviceId)
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.NotFound(""))))
        )
      }
        .provide(
          stubDeviceRepository,
          stubPendingUpdateRepository,
          InMemoryManufacturerRepository.layer,
          InMemoryFirmwareRepository.layer,
          FirmwareService.layer,
          stubFirmwareDownloader,
          PendingUpdateService.layer,
          mockEmailService,
          RestartDevice.layer,
          GetDeviceStatus.layer,
          FlashDevice.layer,
          GetDeviceVersion.layer,
          OtaService.layer,
          OtaApi.layer,
          DeviceProxyRegistry.layer,
          manufacturerRegistryLayer,
        )
    },
    test("Fail with 500 (Internal Server Error) when there is an exception while fetching the device") {
      val mockEmailService = MockEmailService.empty
      {
        for {
          response <- getDeviceVersion(device1.id)
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.InternalServerError("message"))))
        )
      }
        .provide(
          stubDeviceRepositoryThatThrowsException,
          stubPendingUpdateRepository,
          InMemoryManufacturerRepository.layer,
          InMemoryFirmwareRepository.layer,
          FirmwareService.layer,
          stubFirmwareDownloader,
          PendingUpdateService.layer,
          mockEmailService,
          RestartDevice.layer,
          GetDeviceStatus.layer,
          FlashDevice.layer,
          GetDeviceVersion.layer,
          OtaService.layer,
          OtaApi.layer,
          DeviceProxyRegistry.layer,
          manufacturerRegistryLayer,
        )
    },
    test("Fail with 412 (Precondition Failed) when device manufacturer is not supported") {
      val mockEmailService = MockEmailService.empty
      {
        for {
          _ <- givenDevices(device1.copy(manufacturer = unsupportedManufacturer))
          response <- getDeviceVersion(device1.id)
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.PreconditionFailed(s"Manufacturer not supported: $unsupportedManufacturer"))))
        )
      }
        .provide(
          stubDeviceRepository,
          stubPendingUpdateRepository,
          InMemoryManufacturerRepository.layer,
          InMemoryFirmwareRepository.layer,
          FirmwareService.layer,
          stubFirmwareDownloader,
          PendingUpdateService.layer,
          mockEmailService,
          RestartDevice.layer,
          GetDeviceStatus.layer,
          FlashDevice.layer,
          GetDeviceVersion.layer,
          OtaService.layer,
          OtaApi.layer,
          DeviceProxyRegistry.layer,
          manufacturerRegistryLayer,
        )
    },
    test("Fail with 502 (Bad Gateway) when there is an exception while calling the device") {
      val mockEmailService = MockEmailService.empty
      {
        for {
          _ <- givenDevices(device1.copy(manufacturer = manufacturerWithFailingHandler))
          response <- getDeviceVersion(device1.id)
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.BadGateway("error"))))
        )
      }
        .provide(
          stubDeviceRepository,
          stubPendingUpdateRepository,
          InMemoryManufacturerRepository.layer,
          InMemoryFirmwareRepository.layer,
          FirmwareService.layer,
          stubFirmwareDownloader,
          PendingUpdateService.layer,
          mockEmailService,
          RestartDevice.layer,
          GetDeviceStatus.layer,
          FlashDevice.layer,
          GetDeviceVersion.layer,
          OtaService.layer,
          OtaApi.layer,
          DeviceProxyRegistry.layer,
          manufacturerRegistryLayer,
        )
    },
    test("Return the device version") {
      val mockEmailService = MockEmailService.empty
      {
        for {
          _ <- givenDevices(device1)
          device <- getDeviceVersion(device1.id)
            .flatMap(parseResponse[String])
        } yield {
          assert(device)(equalTo("currentFirmwareVersion"))
        }
      }
        .provide(
          stubDeviceRepository,
          stubPendingUpdateRepository,
          InMemoryManufacturerRepository.layer,
          InMemoryFirmwareRepository.layer,
          FirmwareService.layer,
          stubFirmwareDownloader,
          PendingUpdateService.layer,
          mockEmailService,
          RestartDevice.layer,
          GetDeviceStatus.layer,
          FlashDevice.layer,
          GetDeviceVersion.layer,
          OtaService.layer,
          OtaApi.layer,
          DeviceProxyRegistry.layer,
          manufacturerRegistryLayer,
        )
    },
  )
}