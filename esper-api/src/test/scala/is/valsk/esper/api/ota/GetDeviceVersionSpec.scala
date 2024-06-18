package is.valsk.esper.api.ota

import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.device.{DeviceHandler, DeviceProxyRegistry}
import is.valsk.esper.domain.*
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareService, OtaService}
import zio.*
import zio.http.Response
import zio.http.model.HttpError
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object GetDeviceVersionSpec extends ZIOSpecDefault with OtaSpec {

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
        response <- getDeviceVersion(device1.id)
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
    test("Fail with 412 (Precondition Failed) when device manufacturer is not supported") {
      for {
        _ <- givenDevices(device1.copy(manufacturer = unsupportedManufacturer))
        response <- getDeviceVersion(device1.id)
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
        _ <- givenDevices(device1.copy(manufacturer = manufacturerWithFailingHandler))
        response <- getDeviceVersion(device1.id)
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
    test("Return the device version") {
      for {
        _ <- givenDevices(device1)
        device <- getDeviceVersion(device1.id)
          .flatMap(parseResponse[String])
      } yield {
        assert(device)(equalTo("currentFirmwareVersion"))
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
}