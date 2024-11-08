package is.valsk.esper.api.ota

import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.ota.GetDeviceVersionSpec.unsupportedManufacturer
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.ctx.DeviceCtx
import is.valsk.esper.device.*
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.*
import sttp.model.StatusCode
import zio.mock.{Expectation, Mock}
import zio.test.*
import zio.test.Assertion.*
import zio.{ZLayer, *}

object RestartDeviceSpec extends ZIOSpecDefault with OtaSpec with DeviceCtx {

  def spec = suite("RestartDeviceSpec")(
    test("Return a 404 (Not Found) if the device does not exist") {
      val mockDeviceHandler = MockDeviceHandler.empty
      {
        for {
          _ <- givenDevices(device1)
          response <- restartDevice(nonExistentDeviceId)
        } yield {
          assert(response.code)(equalTo(StatusCode.NotFound)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("Not found")))
        }
      }
        .provide(
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
          mockDeviceHandler,
          ZLayer {
            for {
              mockDeviceHandler <- ZIO.service[DeviceHandler]
            } yield Seq(
              mockDeviceHandler,
            )
          },
          stubDeviceEventProducer,
        )
    },
    test("Fail with 500 (Internal Server Error) when there is an exception while fetching the device") {
      val mockDeviceHandler = MockDeviceHandler.empty
      {
        for {
          response <- restartDevice(device1.id)
        } yield {
          assert(response.code)(equalTo(StatusCode.InternalServerError)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("message")))
        }
      }
        .provide(
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
          mockDeviceHandler,
          ZLayer {
            for {
              mockDeviceHandler <- ZIO.service[DeviceHandler]
            } yield Seq(
              mockDeviceHandler,
            )
          },
        )
    },
    test("Fail with 412 (Precondition Failed) when device manufacturer is not supported") {
      val mockDeviceHandler = MockDeviceHandler.empty
      {
        for {
          _ <- givenDevices(device1.copy(manufacturer = unsupportedManufacturer))
          response <- restartDevice(device1.id)
        } yield {
          assert(response.code)(equalTo(StatusCode.PreconditionFailed)) &&
            assert(response.body.swap.toOption)(isSome(equalTo(s"Manufacturer not supported: $unsupportedManufacturer")))
        }
      }
        .provide(
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
          mockDeviceHandler,
          ZLayer {
            for {
              mockDeviceHandler <- ZIO.service[DeviceHandler]
            } yield Seq(
              mockDeviceHandler,
            )
          },
          stubDeviceEventProducer,
        )
    },
    test("Fail with 502 (Bad Gateway) when there is an exception while calling the device") {
      val mockDeviceHandler = MockDeviceHandler.Restart(
        assertion = Assertion.equalTo(device1),
        result = Expectation.failure(ApiCallFailed("error", device1))
      )
      {
        for {
          _ <- givenDevices(device1)
          response <- restartDevice(device1.id)
        } yield {
          assert(response.code)(equalTo(StatusCode.BadGateway)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("error")))
        }
      }
        .provide(
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
          mockDeviceHandler,
          ZLayer {
            for {
              mockDeviceHandler <- ZIO.service[DeviceHandler]
            } yield Seq(
              mockDeviceHandler,
            )
          },
          stubDeviceEventProducer,
        )
    },
    test("Restart the device") {
      val mockDeviceHandler = MockDeviceHandler.Restart(
        assertion = Assertion.equalTo(device1),
        result = Expectation.unit
      )
      {
        for {
          _ <- givenDevices(device1)
          _ <- restartDevice(device1.id)
        } yield assertTrue(true)
      }
        .provide(
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
          mockDeviceHandler,
          ZLayer {
            for {
              mockDeviceHandler <- ZIO.service[DeviceHandler]
            } yield Seq(
              mockDeviceHandler,
            )
          },
          stubDeviceEventProducer,
        )
    },
  )

  object MockDeviceHandler extends Mock[DeviceHandler] {
    object Restart extends Effect[Device, DeviceApiError, Unit]

    val compose: URLayer[mock.Proxy, DeviceHandler] =
      ZLayer {
        for {
          proxy <- ZIO.service[mock.Proxy]
        } yield new DeviceHandler {
          override def getFirmwareDownloadDetails(
              manufacturer: Manufacturer,
              model: Model,
              version: Option[Version]
          ): IO[FirmwareDownloadError, DeviceManufacturerHandler.FirmwareDescriptor] = ???

          override def restartDevice(device: Device): IO[DeviceApiError, Unit] = proxy(Restart, device)

          override def versionOrdering: Ordering[Version] = ???

          override def toDomain(hassDevice: HassResult): IO[MalformedVersion | ParseError, Device] = ???

          override def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, Version] = ???

          override def flashFirmware(device: Device, firmware: Firmware): IO[DeviceApiError, FlashResult] = ???

          override def getDeviceStatus(device: Device): IO[DeviceApiError, DeviceStatus] = ???

          override def parseVersion(version: String): Either[MalformedVersion, Version] = ???

          override def supportedManufacturer: Manufacturer = manufacturer1
        }
      }
  }
}