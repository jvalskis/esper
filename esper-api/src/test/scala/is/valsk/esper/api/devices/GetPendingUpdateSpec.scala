package is.valsk.esper.api.devices

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.model.api.PendingUpdate
import is.valsk.esper.repositories.DeviceRepository
import zio.*
import zio.http.Response
import zio.http.model.HttpError
import zio.test.*
import zio.test.Assertion.*

object GetPendingUpdateSpec extends ZIOSpecDefault with ApiSpec {

  def spec = suite("GetPendingUpdateSpec")(
    test("Return a 404 (Not Found) if the pending update does not exist") {
      for {
        _ <- givenPendingUpdates(pendingUpdate1)
        response <- getPendingUpdate(nonExistentDeviceId)
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.NotFound(nonExistentDeviceId.toString))))
      )
    }
      .provide(
        deviceRepositoryLayerWithTestRepository,
        pendingUpdateRepositoryLayerWithTestRepository,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetPendingUpdate.layer,
        GetPendingUpdates.layer,
      ),

    test("Return the pending update") {
      for {
        _ <- givenPendingUpdates(pendingUpdate1)
        response <- getPendingUpdate(device1.id)
          .flatMap(parseResponse[PendingUpdate])
      } yield {
        assert(response)(equalTo(pendingUpdate1))
      }
    }
      .provide(
        deviceRepositoryLayerWithTestRepository,
        pendingUpdateRepositoryLayerWithTestRepository,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetPendingUpdate.layer,
        GetPendingUpdates.layer,
      ),

    test("Fail with 500 (Internal Server Error) when there is an exception while fetching the pending update") {
      for {
        response <- getPendingUpdate(device1.id)
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.InternalServerError())))
      )
    }
      .provide(
        deviceRepositoryLayerWithTestRepository,
        pendingUpdateRepositoryLayerThatThrowsException,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetPendingUpdate.layer,
        GetPendingUpdates.layer,
      ),
  )
}