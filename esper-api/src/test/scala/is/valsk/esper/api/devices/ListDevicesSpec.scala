package is.valsk.esper.api.devices

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.domain.Device
import is.valsk.esper.repositories.{DeviceRepository, InMemoryPendingUpdateRepository}
import zio.*
import zio.http.model.HttpError
import zio.http.Response
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object ListDevicesSpec extends ZIOSpecDefault with ApiSpec {

  def spec = suite("ListDevicesSpec")(
    test("Return an empty list if there are no devices") {
      for {
        result <- listDevices
          .flatMap(parseResponse[List[Device]])
      } yield assert(result)(isEmpty)
    }
      .provide(
        stubDeviceRepository,
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
      .provide(
        stubDeviceRepository,
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
      .provide(
        stubDeviceRepositoryThatThrowsException,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetPendingUpdate.layer,
        GetPendingUpdates.layer,
        InMemoryPendingUpdateRepository.layer,
      ),
  )
}