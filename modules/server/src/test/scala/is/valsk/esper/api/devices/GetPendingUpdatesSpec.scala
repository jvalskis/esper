package is.valsk.esper.api.devices

import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.domain.PendingUpdate
import is.valsk.esper.repositories.DeviceRepository
import sttp.model.StatusCode
import zio.*
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object GetPendingUpdatesSpec extends ZIOSpecDefault with DevicesSpec {

  def spec = suite("GetPendingUpdatesSpec")(
    suite("Normal flow")(
      test("Return an empty list if there are no pending updates") {
        for {
          response <- getPendingUpdates
          result = response.body.toSeq.flatMap(_.fromJson[List[PendingUpdate]].toSeq).flatten
        } yield assert(result)(isEmpty)
      },
      test("Return the pending update") {
        for {
          _ <- givenPendingUpdates(pendingUpdate1)
          response <- getPendingUpdates
          result = response.body.toSeq.flatMap(_.fromJson[List[PendingUpdate]].toSeq).flatten
        } yield {
          assert(result)(contains(pendingUpdate1))
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
        stubDeviceEventProducer,
      ),

    test("Fail with 500 (Internal Server Error) when there is an exception while fetching the pending update") {
      for {
        response <- getPendingUpdates
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
        stubDeviceEventProducer,
      ),
  )
}