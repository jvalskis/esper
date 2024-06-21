package is.valsk.esper.api.firmware

import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.firmware.endpoints.*
import is.valsk.esper.device.*
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.*
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareDownloader, FirmwareService, PendingUpdateService}
import sttp.model.StatusCode
import zio.*
import zio.test.*
import zio.test.Assertion.*

object DownloadLatestFirmwareSpec extends ZIOSpecDefault with FirmwareSpec {

  def spec = suite("DownloadLatestFirmwareSpec")(
    suite("Normal flow")(
      test("Fail with 412 (Precondition Failed) when device manufacturer is not supported") {
        for {
          response <- downloadLatestFirmware(unsupportedManufacturer, model1)
        } yield {
          assert(response.code)(equalTo(StatusCode.PreconditionFailed)) &&
          assert(response.body.swap.toOption)(isSome(equalTo(s"Manufacturer not supported: $unsupportedManufacturer")))
        }
      },
      test("Download the latest firmware") {
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
          response <- downloadLatestFirmware(manufacturer1, model1)
          firmwareDownloaderProbe <- ZIO.service[FirmwareDownloaderProbe]
          invocations <- firmwareDownloaderProbe.probeInvocations.get
        } yield {
          assert(invocations)(contains(firmwareDescriptor))
        }
      },
      test("Send email when latest firmware is downloaded and there are new pending updates") {
        for {
          _ <- givenDevices(
            device1
          )
          _ <- givenFirmwares(
            Firmware(
              manufacturer = device1.manufacturer,
              model = device1.model,
              version = Version("version1"),
              data = Array.emptyByteArray,
              size = 0,
            ),
          )
          response <- downloadLatestFirmware(manufacturer1, model1)
          emailServiceProbe <- ZIO.service[EmailServiceProbe]
          invocations <- emailServiceProbe.probeInvocations.get
        } yield {
          assert(invocations)(contains((
            "Esper: there are 1 pending updates",
            """
              |<p>The following devices have pending firmware updates:</p>
              |<ul>
              |<li>test-device-1 model1 (id): Version(version2)</li>
              |</ul>
              |""".stripMargin
          )))
        }
      },
      test("Send no email if latest firmware is downloaded but there are no new pending updates") {
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
          response <- downloadLatestFirmware(manufacturer1, model1)
          emailServiceProbe <- ZIO.service[EmailServiceProbe]
          invocations <- emailServiceProbe.probeInvocations.get
        } yield {
          assert(invocations)(isEmpty)
        }
      },
      test("Skip download if latest firmware is already downloaded") {
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
          )
          response <- downloadLatestFirmware(manufacturer1, model1)
          firmwareDownloaderProbe <- ZIO.service[FirmwareDownloaderProbe]
          invocations <- firmwareDownloaderProbe.probeInvocations.get
        } yield {
          assert(invocations)(isEmpty)
        }
      },
      test("Send no email if latest firmware is already downloaded") {
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
          )
          response <- downloadLatestFirmware(manufacturer1, model1)
          emailServiceProbe <- ZIO.service[EmailServiceProbe]
          invocations <- emailServiceProbe.probeInvocations.get
        } yield {
          assert(invocations)(isEmpty)
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
      stubEmailService,
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
        response <- downloadLatestFirmware(manufacturer1, model1)
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
      stubEmailService,
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