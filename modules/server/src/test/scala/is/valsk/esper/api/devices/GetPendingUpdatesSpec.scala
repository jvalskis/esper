package is.valsk.esper.api.devices

import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.domain.PendingUpdate
import is.valsk.esper.repositories.DeviceRepository
import zio.*
import zio.http.Response
import zio.http.model.HttpError
import zio.test.*
import zio.test.Assertion.*

object GetPendingUpdatesSpec extends ZIOSpecDefault with DevicesSpec {

  def spec = suite("GetPendingUpdatesSpec")(
    suite("Normal flow")(
      test("Return an empty list if there are no pending updates") {
        for {
          response <- getPendingUpdates
            .flatMap(parseResponse[List[PendingUpdate]])
        } yield assert(response)(isEmpty)
      },
      test("Return the pending update") {
        for {
          _ <- givenPendingUpdates(pendingUpdate1)
          response <- getPendingUpdates
            .flatMap(parseResponse[List[PendingUpdate]])
        } yield {
          assert(response)(contains(pendingUpdate1))
        }
      }
    )
      .provide(
        stubDeviceRepository,
        stubPendingUpdateRepository,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetPendingUpdate.layer,
        GetPendingUpdates.layer,
      ),

    test("Fail with 500 (Internal Server Error) when there is an exception while fetching the pending update") {
      for {
        response <- getPendingUpdates
      } yield assert(response)(
        fails(isSome(equalTo(HttpError.InternalServerError())))
      )
    }
      .provide(
        stubDeviceRepository,
        stubPendingUpdateRepositoryThatThrowsException,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetPendingUpdate.layer,
        GetPendingUpdates.layer,
      ),
  )
}