package is.valsk.esper.api.devices

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.api.ApiSpec
import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model}
import is.valsk.esper.domain.{Device, DeviceModel, FailedToStoreFirmware, PersistenceException}
import is.valsk.esper.repositories.{DeviceRepository, InMemoryDeviceRepository, InMemoryPendingUpdateRepository}
import zio.*
import zio.http.model.HttpError
import zio.http.Response
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

import java.io.IOException

object ListDevicesSpec extends ZIOSpecDefault with ApiSpec {

  def spec = suite("ListDevicesSpec")(
    test("Return an empty list if there are no devices") {
      for {
        result <- listDevices
          .flatMap(parseResponse[List[Device]])
      } yield assert(result)(isEmpty)
    }
      .provide(
        deviceRepositoryLayerWithTestRepository,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetPendingUpdate.layer,
        GetPendingUpdates.layer,
        InMemoryPendingUpdateRepository.layer,
      ),
    test("Return all devices when there are some") {
      for {
        _ <- givenDevices(device1)
        result <- listDevices
          .flatMap(parseResponse[List[Device]])
      } yield {
        assert(result)(contains(device1))
      }
    }
      .provide(deviceRepositoryLayerWithTestRepository,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetPendingUpdate.layer,
        GetPendingUpdates.layer,
        InMemoryPendingUpdateRepository.layer,
      ),
    test("Fail with 500 (Internal Server Error) when there is an exception while fetching devices") {
      for {
        response <- listDevices
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.InternalServerError())))
      )
    }
      .provide(deviceRepositoryLayerThatThrowsException,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetPendingUpdate.layer,
        GetPendingUpdates.layer,
        InMemoryPendingUpdateRepository.layer,
      ),
  )

  val deviceRepositoryLayerWithTestRepository: ULayer[DeviceRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[NonEmptyString, Device])
    } yield InMemoryDeviceRepository(ref)
  }

  val deviceRepositoryLayerThatThrowsException: ULayer[DeviceRepository] = ZLayer.succeed(
    new DeviceRepository {
      override def get(id: DeviceId): IO[PersistenceException, Device] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))

      override def getOpt(id: DeviceId): IO[PersistenceException, Option[Device]] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))

      override def getAll: IO[PersistenceException, List[Device]] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))

      override def add(device: Device): IO[PersistenceException, Device] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))

      override def update(device: Device): IO[PersistenceException, Device] = ZIO.fail(FailedToStoreFirmware("message", DeviceModel(Model.unsafeFrom("model"), Manufacturer.unsafeFrom("manufacturer")), Some(IOException("test"))))
    }
  )
}