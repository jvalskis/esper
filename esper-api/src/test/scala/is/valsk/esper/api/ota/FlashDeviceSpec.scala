package is.valsk.esper.api.ota

import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.device.*
import is.valsk.esper.device.DeviceStatus.UpdateStatus
import is.valsk.esper.domain.*
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareService, OtaService}
import zio.*
import zio.http.Response
import zio.http.model.HttpError
import zio.test.*
import zio.test.Assertion.*

object FlashDeviceSpec extends ZIOSpecDefault with OtaSpec {

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
          _ <- givenFirmwares(Firmware(
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
          _ <- givenFirmwares(Firmware(
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
          _ <- givenFirmwares(Firmware(
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
          _ <- givenFirmwares(Firmware(
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
    ),
    suite("with explicit version")(
      test("Return a 404 (Not Found) if the device does not exist") {
        for {
          _ <- givenDevices(device1)
          response <- flashDevice(nonExistentDeviceId, Version("version"))
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
          response <- flashDevice(device1.id, Version("version"))
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
      test("Fail with 412 (Precondition Failed) when specific firmware is not found (no firmwares exist)") {
        for {
          _ <- givenDevices(device1)
          response <- flashDevice(device1.id, Version("version"))
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.PreconditionFailed(s"Firmware not found"))))
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
      test("Fail with 412 (Precondition Failed) when specific firmware is not found (version mismatch)") {
        for {
          _ <- givenFirmwares(Firmware(
            manufacturer = device1.manufacturer,
            model = device1.model,
            version = Version("version"),
            data = Array.emptyByteArray,
            size = 0,
          ))
          _ <- givenDevices(device1)
          response <- flashDevice(device1.id, Version("other-version"))
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.PreconditionFailed(s"Firmware not found"))))
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
          _ <- givenFirmwares(Firmware(
            manufacturer = otherManufacturer,
            model = device1.model,
            version = Version("version"),
            data = Array.emptyByteArray,
            size = 0,
          ))
          _ <- givenDevices(device1)
          response <- flashDevice(device1.id, Version("version"))
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.PreconditionFailed(s"Firmware not found"))))
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
          _ <- givenFirmwares(Firmware(
            manufacturer = device1.manufacturer,
            model = otherModel,
            version = Version("version"),
            data = Array.emptyByteArray,
            size = 0,
          ))
          _ <- givenDevices(device1)
          response <- flashDevice(device1.id, Version("version"))
        } yield assert(response)(
          fails(isSome(equalTo(HttpError.PreconditionFailed(s"Firmware not found"))))
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
          _ <- givenFirmwares(Firmware(
            manufacturer = manufacturerWithFailingHandler,
            model = device1.model,
            version = Version("version"),
            data = Array.emptyByteArray,
            size = 0,
          ))
          _ <- givenDevices(device1.copy(manufacturer = manufacturerWithFailingHandler))
          response <- flashDevice(device1.id, Version("version"))
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
      test("Flash the device with the specific firmware") {
        for {
          _ <- givenFirmwares(
            Firmware(
              manufacturer = device1.manufacturer,
              model = device1.model,
              version = Version("version1"),
              data = Array.emptyByteArray,
              size = 0,
            ),
            Firmware(
              manufacturer = device1.manufacturer,
              model = device1.model,
              version = Version("version2"),
              data = Array.emptyByteArray,
              size = 0,
            ),
            Firmware(
              manufacturer = device1.manufacturer,
              model = device1.model,
              version = Version("version3"),
              data = Array.emptyByteArray,
              size = 0,
            )
          )
          _ <- givenDevices(device1)
          device <- flashDevice(device1.id, Version("version2"))
            .flatMap(parseResponse[FlashResult])
        } yield {
          assert(device)(equalTo(FlashResult(
            previousVersion = Version("previousVersion"),
            currentVersion = Version("version2"),
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
}