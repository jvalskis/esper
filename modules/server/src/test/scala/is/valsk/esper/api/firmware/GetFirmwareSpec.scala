package is.valsk.esper.api.firmware

import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.firmware.endpoints.{DeleteFirmware, DownloadFirmware, DownloadLatestFirmware, GetFirmware, ListFirmwareVersions}
import is.valsk.esper.device.*
import is.valsk.esper.domain.*
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareDownloader, FirmwareService, PendingUpdateService}
import sttp.model.StatusCode
import zio.*
import zio.test.*
import zio.test.Assertion.*

object GetFirmwareSpec extends ZIOSpecDefault with FirmwareSpec {

  def spec = suite("GetFirmwareSpec")(
    suite("Normal flow")(
      test("Return a 404 (Not Found) if the latest firmware does not exist") {
        for {
          response <- getFirmware(manufacturer1, model1)
        } yield {
          assert(response.code)(equalTo(StatusCode.NotFound)) &&
          assert(response.body.swap.toOption)(isSome(equalTo(s"Latest firmware not found")))
        }
      },
      test("Return a 404 (Not Found) if the specific firmware version does not exist") {
        for {
          _ <- givenFirmwares(
            Firmware(
              manufacturer = device1.manufacturer,
              model = device1.model,
              version = Version("version1"),
              data = Array.emptyByteArray,
              size = 0,
            ),
          )
          response <- getFirmware(manufacturer1, model1, Version("version2"))
        } yield {
          assert(response.code)(equalTo(StatusCode.NotFound)) &&
          assert(response.body.swap.toOption)(isSome(equalTo(s"Firmware not found")))
        }
      },
      test("Fail with 412 (Precondition Failed) when device manufacturer is not supported") {
        for {
          response <- getFirmware(unsupportedManufacturer, model1)
        } yield {
          assert(response.code)(equalTo(StatusCode.PreconditionFailed)) &&
          assert(response.body.swap.toOption)(isSome(equalTo(s"Manufacturer not supported: $unsupportedManufacturer")))
        }
      },
      test("Return the latest firmware") {
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
              version = Version("version3"),
              data = "version3".getBytes,
              size = 0,
            ),
            Firmware(
              manufacturer = device1.manufacturer,
              model = device1.model,
              version = Version("version2"),
              data = Array.emptyByteArray,
              size = 0,
            )
          )
          response <- getFirmware(manufacturer1, model1)
//          body <- response.map(_.body.asChunk).flatten
        } yield {
//          assert(response.headers.find(_.name == "Content-Type"))(isSome(equalTo("application/zip"))) && TODO fix content type
//          assert(response.body.toOption.map(_.getBytes))(isSome(equalTo(Chunk.fromArray("version3".getBytes))))
            assert(response.headers.find(_.name == "Content-Type").map(_.value))(isSome(equalTo("application/octet-stream"))) &&
              assert(response.body.toOption.map(_.getBytes))(isSome(equalTo("version3".getBytes)))
        }
      },
      test("Return specific firmware version") {
        for {
          _ <- givenFirmwares(
            Firmware(
              manufacturer = device1.manufacturer,
              model = device1.model,
              version = Version("version1"),
              data = "version1".getBytes,
              size = 0,
            ),
            Firmware(
              manufacturer = device1.manufacturer,
              model = device1.model,
              version = Version("version3"),
              data = Array.emptyByteArray,
              size = 0,
            ),
            Firmware(
              manufacturer = device1.manufacturer,
              model = device1.model,
              version = Version("version2"),
              data = Array.emptyByteArray,
              size = 0,
            )
          )
          response <- getFirmware(manufacturer1, model1, Version("version1"))
//          body <- response.map(_.body.asChunk).flatten
        } yield {
//          assert(response.headers.find(_.name == "Content-Type"))(isSome(equalTo("application/zip"))) && TODO fix content type
//          assert(response.body)(equalTo(Chunk.fromArray("version1".getBytes)))
          assert(response.headers.find(_.name == "Content-Type").map(_.value))(isSome(equalTo("application/octet-stream"))) &&
            assert(response.body.toOption.map(_.getBytes))(isSome(equalTo("version1".getBytes)))
        }
      }
    ).provide(
      stubDeviceRepository,
      InMemoryManufacturerRepository.layer,
      InMemoryFirmwareRepository.layer,
      FirmwareService.layer,
      stubFirmwareDownloader,
      PendingUpdateService.layer,
      stubPendingUpdateRepository,
      MockEmailService.empty,
      DeleteFirmware.layer,
      DownloadFirmware.layer,
      DownloadLatestFirmware.layer,
      GetFirmware.layer,
      ListFirmwareVersions.layer,
      FirmwareApi.layer,
      stubManufacturerRegistryLayer,
    ),
    test("Fail with 500 (Internal Server Error) when there is an exception while fetching the device") {
      for {
        response <- getFirmware(manufacturer1, model1)
      } yield {
        assert(response.code)(equalTo(StatusCode.InternalServerError)) &&
        assert(response.body.swap.toOption)(isSome(equalTo("message")))
      }
    }.provide(
      stubDeviceRepositoryThatThrowsException,
      InMemoryManufacturerRepository.layer,
      stubFirmwareRepositoryThatThrowsException,
      FirmwareService.layer,
      stubFirmwareDownloader,
      PendingUpdateService.layer,
      stubPendingUpdateRepository,
      MockEmailService.empty,
      DeleteFirmware.layer,
      DownloadFirmware.layer,
      DownloadLatestFirmware.layer,
      GetFirmware.layer,
      ListFirmwareVersions.layer,
      FirmwareApi.layer,
      stubManufacturerRegistryLayer,
    )
  )
}