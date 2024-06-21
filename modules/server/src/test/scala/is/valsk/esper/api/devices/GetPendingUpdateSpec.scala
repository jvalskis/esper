package is.valsk.esper.api.devices

import is.valsk.esper.api.devices.GetDeviceSpec.{nonExistentDeviceId, test}
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.domain.PendingUpdate
import is.valsk.esper.repositories.DeviceRepository
import sttp.model.StatusCode
import zio.*
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object GetPendingUpdateSpec extends ZIOSpecDefault with DevicesSpec {

  def spec = suite("GetPendingUpdateSpec")(
    suite("Normal flow")(
      test("Return a 404 (Not Found) if the pending update does not exist") {
        for {
          _ <- givenPendingUpdates(pendingUpdate1)
          response <- getPendingUpdate(nonExistentDeviceId)
        } yield {
          assert(response.code)(equalTo(StatusCode.NotFound)) &&
          assert(response.body.swap.toOption)(isSome(equalTo("Not found")))
        }
      },
      test("Return the pending update") {
        for {
          _ <- givenPendingUpdates(pendingUpdate1)
          response <- getPendingUpdate(device1.id)
          result = response.body.toOption.flatMap(_.fromJson[PendingUpdate].toOption)
        } yield {
          assert(result)(isSome(equalTo(pendingUpdate1)))
        }
      },
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
        response <- getPendingUpdate(device1.id)
      } yield {
        assert(response.code)(equalTo(StatusCode.InternalServerError)) &&
        assert(response.body.swap.toOption)(isSome(equalTo("message")))
      }
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