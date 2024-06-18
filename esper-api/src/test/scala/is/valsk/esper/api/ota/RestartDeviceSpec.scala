package is.valsk.esper.api.ota

import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.device.*
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareService, OtaService}
import zio.{mock, *}
import zio.http.Response
import zio.http.model.HttpError
import zio.mock.{Expectation, Mock}
import zio.test.*
import zio.test.Assertion.*

object RestartDeviceSpec extends ZIOSpecDefault with OtaSpec {

  def spec = suite("RestartDeviceSpec")(
    test("Return a 404 (Not Found) if the device does not exist") {
      for {
        _ <- givenDevices(device1)
        response <- restartDevice(nonExistentDeviceId)
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
        response <- restartDevice(device1.id)
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
        response <- restartDevice(device1.id)
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
        response <- restartDevice(device1.id)
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
    test("Restart the device") {
      val mockDeviceHandler: Expectation[DeviceHandler] = MockDeviceHandler.Restart(
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
          mockDeviceHandler,
          ZLayer {
            for {
              mockDeviceHandler <- ZIO.service[DeviceHandler]
            } yield Map(
              manufacturer1.toString -> mockDeviceHandler,
            )
          }
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
        }
      }
  }
}