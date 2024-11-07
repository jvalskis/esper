package is.valsk.esper.api.ota

import is.valsk.esper.api.devices.GetDeviceSpec.{stubDeviceEventProducer, test}
import is.valsk.esper.api.firmware.DownloadLatestFirmwareSpec.unsupportedManufacturer
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.device.*
import is.valsk.esper.domain
import is.valsk.esper.domain.*
import is.valsk.esper.domain.DeviceStatus.UpdateStatus
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareDownloader, FirmwareService, OtaService}
import sttp.model.StatusCode
import zio.*
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object FlashDeviceSpec extends ZIOSpecDefault with OtaSpec {

  def spec = suite("FlashDeviceSpec")(
    suite("without explicit version")(
      suite("Normal flow")(
        test("Return a 404 (Not Found) if the device does not exist") {
          for {
            _ <- givenDevices(device1)
            response <- flashDevice(nonExistentDeviceId)
          } yield {
            assert(response.code)(equalTo(StatusCode.NotFound)) &&
              assert(response.body.swap.toOption)(isSome(equalTo("Not found")))
          }
        },
        test("Fail with 412 (Precondition Failed) when latest firmware is not found") {
          for {
            _ <- givenDevices(device1)
            response <- flashDevice(device1.id)
          } yield {
//            assert(response.code)(equalTo(StatusCode.PreconditionFailed)) && // TODO fix status code
            assert(response.code)(equalTo(StatusCode.NotFound)) &&
              assert(response.body.swap.toOption)(isSome(equalTo("Latest firmware not found")))
          }
        },
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
          } yield {
            //            assert(response.code)(equalTo(StatusCode.PreconditionFailed)) && // TODO fix status code
            assert(response.code)(equalTo(StatusCode.NotFound)) &&
              assert(response.body.swap.toOption)(isSome(equalTo("Latest firmware not found")))
          }
        },
        test("Fail with 412 (Precondition Failed) when latest firmware is not found (mismatching model)") {
          for {
            _ <- givenFirmwares(domain.Firmware(
              manufacturer = device1.manufacturer,
              model = otherModel,
              version = Version("version"),
              data = Array.emptyByteArray,
              size = 0,
            ))
            _ <- givenDevices(device1)
            response <- flashDevice(device1.id)
          } yield {
            //            assert(response.code)(equalTo(StatusCode.PreconditionFailed)) && // TODO fix status code
            assert(response.code)(equalTo(StatusCode.NotFound)) &&
              assert(response.body.swap.toOption)(isSome(equalTo("Latest firmware not found")))
          }
        },
        test("Fail with 412 (Precondition Failed) when manufacturer is not supported") {
          for {
            _ <- givenDevices(device1.copy(manufacturer = unsupportedManufacturer))
            response <- flashDevice(device1.id)
          } yield {
            assert(response.code)(equalTo(StatusCode.PreconditionFailed)) &&
              assert(response.body.swap.toOption)(isSome(equalTo(s"Manufacturer not supported: $unsupportedManufacturer")))
          }
        },
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
          } yield {
            assert(response.code)(equalTo(StatusCode.BadGateway)) &&
              assert(response.body.swap.toOption)(isSome(equalTo("error")))
          }
        },
        test("Flash the device with the latest firmware") {
          {
            for {
              _ <- givenFirmwares(Firmware(
                manufacturer = device1.manufacturer,
                model = device1.model,
                version = Version("currentVersion"),
                data = Array.emptyByteArray,
                size = 0,
              ))
              _ <- givenDevices(device1)
              response <- flashDevice(device1.id)
              result = response.body.toOption.flatMap(_.fromJson[FlashResult].toOption)
            } yield {
              assert(result)(isSome(equalTo(FlashResult(
                previousVersion = Version("previousVersion"),
                currentVersion = Version("currentVersion"),
                updateStatus = UpdateStatus.done
              ))))
            }
          }
        }
      ).provide(
        stubDeviceRepository,
        InMemoryManufacturerRepository.layer,
        InMemoryFirmwareRepository.layer,
        FirmwareService.layer,
        stubFirmwareDownloader,
        RestartDevice.layer,
        GetDeviceStatus.layer,
        FlashDevice.layer,
        GetDeviceVersion.layer,
        OtaService.layer,
        OtaApi.layer,
        DeviceProxyRegistry.layer,
        stubManufacturerRegistryLayer,
        stubDeviceEventProducer,
      ),
      test("Fail with 500 (Internal Server Error) when there is an exception while fetching the device") {
        for {
          response <- flashDevice(device1.id)
        } yield {
          assert(response.code)(equalTo(StatusCode.InternalServerError)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("message")))
        }
      }.provide(
        stubDeviceRepositoryThatThrowsException,
        InMemoryManufacturerRepository.layer,
        InMemoryFirmwareRepository.layer,
        FirmwareService.layer,
        stubFirmwareDownloader,
        RestartDevice.layer,
        GetDeviceStatus.layer,
        FlashDevice.layer,
        GetDeviceVersion.layer,
        OtaService.layer,
        OtaApi.layer,
        DeviceProxyRegistry.layer,
        stubManufacturerRegistryLayer,
      ),
    ),
    suite("with explicit version")(
      suite("Normal flow")(
        test("Return a 404 (Not Found) if the device does not exist") {
          for {
            _ <- givenDevices(device1)
            response <- flashDevice(nonExistentDeviceId, Version("version"))
          } yield {
            assert(response.code)(equalTo(StatusCode.NotFound)) &&
              assert(response.body.swap.toOption)(isSome(equalTo("Not found")))
          }
        },
        test("Fail with 412 (Precondition Failed) when specific firmware is not found (no firmwares exist)") {
          for {
            _ <- givenDevices(device1)
            response <- flashDevice(device1.id, Version("version"))
          } yield {
            //            assert(response.code)(equalTo(StatusCode.PreconditionFailed)) && // TODO fix status code
            assert(response.code)(equalTo(StatusCode.NotFound)) &&
              assert(response.body.swap.toOption)(isSome(equalTo("Firmware not found")))
          }
        },
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
          } yield {
            //            assert(response.code)(equalTo(StatusCode.PreconditionFailed)) && // TODO fix status code
            assert(response.code)(equalTo(StatusCode.NotFound)) &&
              assert(response.body.swap.toOption)(isSome(equalTo("Firmware not found")))
          }
        },
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
          } yield {
            //            assert(response.code)(equalTo(StatusCode.PreconditionFailed)) && // TODO fix status code
            assert(response.code)(equalTo(StatusCode.NotFound)) &&
              assert(response.body.swap.toOption)(isSome(equalTo("Firmware not found")))
          }
        },
        test("Fail with 412 (Precondition Failed) when latest firmware is not found (mismatching model)") {
          for {
            _ <- givenFirmwares(domain.Firmware(
              manufacturer = device1.manufacturer,
              model = otherModel,
              version = Version("version"),
              data = Array.emptyByteArray,
              size = 0,
            ))
            _ <- givenDevices(device1)
            response <- flashDevice(device1.id, Version("version"))
          } yield {
            //            assert(response.code)(equalTo(StatusCode.PreconditionFailed)) && // TODO fix status code
            assert(response.code)(equalTo(StatusCode.NotFound)) &&
              assert(response.body.swap.toOption)(isSome(equalTo("Firmware not found")))
          }
        },
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
          } yield {
            assert(response.code)(equalTo(StatusCode.BadGateway)) &&
              assert(response.body.swap.toOption)(isSome(equalTo("error")))
          }
        },
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
            response <- flashDevice(device1.id, Version("version2"))
            result = response.body.toOption.flatMap(_.fromJson[FlashResult].toOption)
          } yield {
            assert(result)(isSome(equalTo(FlashResult(
              previousVersion = Version("previousVersion"),
              currentVersion = Version("version2"),
              updateStatus = UpdateStatus.done
            ))))
          }
        },
      ).provide(
        stubDeviceRepository,
        InMemoryManufacturerRepository.layer,
        InMemoryFirmwareRepository.layer,
        FirmwareService.layer,
        stubFirmwareDownloader,
        RestartDevice.layer,
        GetDeviceStatus.layer,
        FlashDevice.layer,
        GetDeviceVersion.layer,
        OtaService.layer,
        OtaApi.layer,
        DeviceProxyRegistry.layer,
        stubManufacturerRegistryLayer,
        stubDeviceEventProducer,
      ),

      test("Fail with 500 (Internal Server Error) when there is an exception while fetching the device") {
        for {
          response <- flashDevice(device1.id, Version("version"))
        } yield {
          assert(response.code)(equalTo(StatusCode.InternalServerError)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("message")))
        }
      }.provide(
        stubDeviceRepositoryThatThrowsException,
        InMemoryManufacturerRepository.layer,
        InMemoryFirmwareRepository.layer,
        FirmwareService.layer,
        stubFirmwareDownloader,
        RestartDevice.layer,
        GetDeviceStatus.layer,
        FlashDevice.layer,
        GetDeviceVersion.layer,
        OtaService.layer,
        OtaApi.layer,
        DeviceProxyRegistry.layer,
        stubManufacturerRegistryLayer,
      )
    )
  )
}