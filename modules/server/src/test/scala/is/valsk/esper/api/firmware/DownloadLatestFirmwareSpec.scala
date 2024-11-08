package is.valsk.esper.api.firmware

import is.valsk.esper.api.firmware.endpoints.*
import is.valsk.esper.ctx.{DeviceCtx, FirmwareCtx}
import is.valsk.esper.device.*
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain.*
import is.valsk.esper.repositories.*
import is.valsk.esper.services.{FirmwareDownloader, FirmwareService}
import sttp.model.StatusCode
import zio.*
import zio.test.*
import zio.test.Assertion.*

object DownloadLatestFirmwareSpec extends ZIOSpecDefault with FirmwareSpec with FirmwareCtx with DeviceCtx {

  def spec = {
    val firmware = Firmware(
      manufacturer = device1.manufacturer,
      model = device1.model,
      version = Version("version1"),
      data = Array.emptyByteArray,
      size = 0,
    )

    suite("DownloadLatestFirmwareSpec")(
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
              firmware,
            )
            response <- downloadLatestFirmware(manufacturer1, model1)
            firmwareDownloaderProbe <- ZIO.service[FirmwareDownloaderProbe]
            invocations <- firmwareDownloaderProbe.probeInvocations.get
          } yield {
            assert(invocations)(contains(firmwareDescriptor))
          }
        },

        test("Skip download if latest firmware is already downloaded") {
          for {
            _ <- givenFirmwares(
              firmware,
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
              firmware,
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
        fixture,
        stubFirmwareDownloader,
        InMemoryFirmwareRepository.layer,
        stubEmailService, 
      ),

      test("Fail with 500 (Internal Server Error) when there is an exception while fetching the device") {
        for {
          response <- downloadLatestFirmware(manufacturer1, model1)
        } yield {
          assert(response.code)(equalTo(StatusCode.InternalServerError)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("message")))
        }
      }.provide(
        fixture,
        stubFirmwareRepositoryThatThrowsException,
      )
    )
  }

  private val fixture = ZLayer.makeSome[FirmwareRepository, FirmwareApi](
    InMemoryManufacturerRepository.layer,
    FirmwareService.layer,
    stubFirmwareDownloader,
    DeleteFirmware.layer,
    DownloadFirmware.layer,
    DownloadLatestFirmware.layer,
    GetFirmware.layer,
    ListFirmwareVersions.layer,
    FirmwareApi.layer,
    stubManufacturerRegistryLayer,
  )
}