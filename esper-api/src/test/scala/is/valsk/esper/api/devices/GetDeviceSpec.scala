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
import zio.test.*
import zio.test.Assertion.*

import java.io.IOException


object GetDeviceSpec extends ZIOSpecDefault with ApiSpec {

  def spec = suite("GetDeviceSpec")(
    test("Return a 404 (Not Found) if the device does not exist") {
      for {
        _ <- givenDevices(device1)
        response <- getDevice(nonExistentDeviceId)
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.NotFound(nonExistentDeviceId.toString))))
      )
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

    test("Return the device") {
      for {
        _ <- givenDevices(device1)
        response <- getDevice(device1.id)
          .flatMap(parseResponse[Device])
      } yield {
        assert(response)(equalTo(device1))
      }
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

    test("Fail with 500 (Internal Server Error) when there is an exception while fetching the device") {
      for {
        response <- getDevice(device1.id)
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.InternalServerError())))
      )
    }
      .provide(
        deviceRepositoryLayerThatThrowsException,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetPendingUpdate.layer,
        GetPendingUpdates.layer,
        InMemoryPendingUpdateRepository.layer,
      ),
  )
}