package is.valsk.esper.api.devices

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.EsperConfig
import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.{ApiSpec, DeviceApi, FirmwareApi}
import is.valsk.esper.device.shelly.{ShellyConfig, ShellyDeviceHandler}
import is.valsk.esper.device.{DeviceManufacturerHandler, DeviceProxy, DeviceProxyRegistry}
import is.valsk.esper.domain.{Device, DeviceModel, FailedToStoreFirmware, PersistenceException}
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
import is.valsk.esper.hass.HassToDomainMapper
import is.valsk.esper.repositories.{DeviceRepository, InMemoryDeviceRepository, InMemoryFirmwareRepository}
import is.valsk.esper.services.HttpClient
import zio.*
import zio.http.model.{HttpError, Method, Status}
import zio.http.{Client, Request, Response, URL}
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

import java.io.IOException

object GetDevicesSpec extends ZIOSpecDefault with ApiSpec {

  def spec = suite("GetDevicesSpec")(
    test("Return an empty list if there are no devices") {
      for {
        result <- getDevices
          .flatMap(parseResponse[List[Device]])
      } yield assert(result)(isEmpty)
    }
      .provide(
        layerWithTestRepository,
        InMemoryFirmwareRepository.layer,
        DeviceApi.layer,
        GetDevice.layer,
        GetDevices.layer,
        GetDeviceVersion.layer,
        FlashDevice.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
        ShellyDeviceHandler.layer,
        HttpClient.layer,
        EsperConfig.layer,
        ShellyConfig.layer,
        Client.default,
      ),
    test("Return all devices when there are some") {
      for {
        _ <- givenDevices(device1)
        result <- getDevices
          .flatMap(parseResponse[List[Device]])
      } yield {
        assert(result)(contains(device1))
      }
    }
      .provide(layerWithTestRepository,
        InMemoryFirmwareRepository.layer,
        DeviceApi.layer,
        GetDevice.layer,
        GetDevices.layer,
        GetDeviceVersion.layer,
        FlashDevice.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
        ShellyDeviceHandler.layer,
        HttpClient.layer,
        EsperConfig.layer,
        ShellyConfig.layer,
        Client.default,
      ),
    test("Fail with 500 (Internal Server Error) when there is an exception while fetching devices") {
      for {
        response <- getDevices
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.InternalServerError())))
      )
    }
      .provide(layerThatThrowsException,
        InMemoryFirmwareRepository.layer,
        DeviceApi.layer,
        GetDevice.layer,
        GetDevices.layer,
        GetDeviceVersion.layer,
        FlashDevice.layer,
        DeviceProxyRegistry.layer,
        manufacturerRegistryLayer,
        ShellyDeviceHandler.layer,
        HttpClient.layer,
        EsperConfig.layer,
        ShellyConfig.layer,
        Client.default,
      ),
  )

  val layerWithTestRepository: ULayer[DeviceRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[NonEmptyString, Device])
    } yield InMemoryDeviceRepository(ref)
  }

  val layerThatThrowsException: ULayer[DeviceRepository] = ZLayer.succeed(
    new DeviceRepository {
      override def get(id: NonEmptyString): IO[PersistenceException, Option[Device]] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))

      override def getAll: IO[PersistenceException, List[Device]] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))

      override def add(device: Device): IO[PersistenceException, Device] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))
    }
  )

  private val manufacturerRegistryLayer: URLayer[ShellyDeviceHandler, Map[Manufacturer, DeviceManufacturerHandler with HassToDomainMapper with DeviceProxy]] = ZLayer {
    for {
      shellyDeviceHandler <- ZIO.service[ShellyDeviceHandler]
    } yield Map(
      shellyDeviceHandler.supportedManufacturer -> shellyDeviceHandler
    )
  }
}