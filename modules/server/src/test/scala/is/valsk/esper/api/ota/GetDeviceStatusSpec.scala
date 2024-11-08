package is.valsk.esper.api.ota

import is.valsk.esper.api.devices.GetDeviceSpec.{stubDeviceEventProducer, test}
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.ctx.DeviceCtx
import is.valsk.esper.device.*
import is.valsk.esper.domain.*
import is.valsk.esper.domain.DeviceStatus.UpdateStatus
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareDownloader, FirmwareService, OtaService}
import sttp.model.StatusCode
import zio.*
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object GetDeviceStatusSpec extends ZIOSpecDefault with OtaSpec with DeviceCtx {

  def spec = suite("GetDeviceStatusSpec")(
    suite("Normal flow")(
      test("Return a 404 (Not Found) if the device does not exist") {
        for {
          _ <- givenDevices(device1)
          response <- getDeviceStatus(nonExistentDeviceId)
        } yield {
          assert(response.code)(equalTo(StatusCode.NotFound)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("Not found")))
        }
      },
      test("Fail with 412 (Precondition Failed) when device manufacturer is not supported") {
        for {
          _ <- givenDevices(device1.copy(manufacturer = unsupportedManufacturer))
          response <- getDeviceStatus(device1.id)
        } yield {
          assert(response.code)(equalTo(StatusCode.PreconditionFailed)) &&
            assert(response.body.swap.toOption)(isSome(equalTo(s"Manufacturer not supported: $unsupportedManufacturer")))
        }
      },
      test("Fail with 502 (Bad Gateway) when there is an exception while calling the device") {
        for {
          _ <- givenDevices(device1.copy(manufacturer = manufacturerWithFailingHandler))
          response <- getDeviceStatus(device1.id)
        } yield {
          assert(response.code)(equalTo(StatusCode.BadGateway)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("error")))
        }
      },
      test("Return the device status") {
        for {
          _ <- givenDevices(device1)
          response <- getDeviceStatus(device1.id)
          result = response.body.toOption.flatMap(_.fromJson[DeviceStatus].toOption)
        } yield {
          assert(result)(isSome(equalTo(DeviceStatus(UpdateStatus.done))))
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
      {
        for {
          response <- getDeviceStatus(device1.id)
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
//        stubDeviceEventProducer,
      )
    }
  )
}