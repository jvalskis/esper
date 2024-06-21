package is.valsk.esper.api.ota

import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.device.{DeviceHandler, DeviceProxyRegistry}
import is.valsk.esper.domain.*
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareDownloader, FirmwareService, OtaService, PendingUpdateService}
import sttp.model.StatusCode
import zio.*
import zio.test.*
import zio.test.Assertion.*

object GetDeviceVersionSpec extends ZIOSpecDefault with OtaSpec {

  def spec = suite("GetDeviceVersionSpec")(
    suite("Normal flow")(
      test("Return a 404 (Not Found) if the device does not exist") {
        for {
          _ <- givenDevices(device1)
          response <- getDeviceVersion(nonExistentDeviceId)
        } yield {
          assert(response.code)(equalTo(StatusCode.NotFound)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("Not found")))
        }
      },
      test("Fail with 412 (Precondition Failed) when device manufacturer is not supported") {
        for {
          _ <- givenDevices(device1.copy(manufacturer = unsupportedManufacturer))
          response <- getDeviceVersion(device1.id)
        } yield {
          assert(response.code)(equalTo(StatusCode.PreconditionFailed)) &&
            assert(response.body.swap.toOption)(isSome(equalTo(s"Manufacturer not supported: $unsupportedManufacturer")))
        }
      },
      test("Fail with 502 (Bad Gateway) when there is an exception while calling the device") {
        for {
          _ <- givenDevices(device1.copy(manufacturer = manufacturerWithFailingHandler))
          response <- getDeviceVersion(device1.id)
        } yield {
          assert(response.code)(equalTo(StatusCode.BadGateway)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("error")))
        }
      },
      test("Return the device version") {
        for {
          _ <- givenDevices(device1)
          response <- getDeviceVersion(device1.id)
          result = response.body.toOption
        } yield {
          assert(result)(isSome(equalTo("\"currentFirmwareVersion\"")))// TODO check why the value is in quotes
        }
      },
    ).provide(
      stubDeviceRepository,
      stubPendingUpdateRepository,
      InMemoryManufacturerRepository.layer,
      InMemoryFirmwareRepository.layer,
      FirmwareService.layer,
      stubFirmwareDownloader,
      PendingUpdateService.layer,
      MockEmailService.empty,
      RestartDevice.layer,
      GetDeviceStatus.layer,
      FlashDevice.layer,
      GetDeviceVersion.layer,
      OtaService.layer,
      OtaApi.layer,
      DeviceProxyRegistry.layer,
      stubManufacturerRegistryLayer,
    ),
    test("Fail with 500 (Internal Server Error) when there is an exception while fetching the device") {
      for {
        response <- getDeviceVersion(device1.id)
      } yield {
        assert(response.code)(equalTo(StatusCode.InternalServerError)) &&
          assert(response.body.swap.toOption)(isSome(equalTo("message")))
      }
    }
      .provide(
        stubDeviceRepositoryThatThrowsException,
        stubPendingUpdateRepository,
        InMemoryManufacturerRepository.layer,
        InMemoryFirmwareRepository.layer,
        FirmwareService.layer,
        stubFirmwareDownloader,
        PendingUpdateService.layer,
        MockEmailService.empty,
        RestartDevice.layer,
        GetDeviceStatus.layer,
        FlashDevice.layer,
        GetDeviceVersion.layer,
        OtaService.layer,
        OtaApi.layer,
        DeviceProxyRegistry.layer,
        stubManufacturerRegistryLayer,
      ),
  )
}