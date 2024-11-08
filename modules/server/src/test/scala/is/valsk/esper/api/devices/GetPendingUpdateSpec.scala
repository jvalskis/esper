package is.valsk.esper.api.devices

import is.valsk.esper.api.devices.GetDeviceSpec.{nonExistentDeviceId, stubDeviceEventProducer, test}
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.ctx.{DeviceCtx, PendingUpdateCtx}
import is.valsk.esper.domain.PendingUpdate
import is.valsk.esper.repositories.{DeviceRepository, PendingUpdateRepository}
import sttp.model.StatusCode
import zio.*
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object GetPendingUpdateSpec extends ZIOSpecDefault with DevicesSpec with DeviceCtx with PendingUpdateCtx {

  def spec = suite("GetPendingUpdateSpec")(
    suite("Normal flow")(
      test("Return a 404 (Not Found) if the pending update does not exist") {
        for {
          _ <- givenPendingUpdates(pendingUpdate1)
          response <- invokeGetPendingUpdate(nonExistentDeviceId)
        } yield {
          assert(response.code)(equalTo(StatusCode.NotFound)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("Not found")))
        }
      },
      test("Return the pending update") {
        for {
          _ <- givenPendingUpdates(pendingUpdate1)
          response <- invokeGetPendingUpdate(device1.id)
          result = response.body.toOption.flatMap(_.fromJson[PendingUpdate].toOption)
        } yield {
          assert(result)(isSome(equalTo(pendingUpdate1)))
        }
      },
    )
      .provide(
        stubPendingUpdateRepository,
        fixture,
      ),
    test("Fail with 500 (Internal Server Error) when there is an exception while fetching the pending update") {
      for {
        response <- invokeGetPendingUpdate(device1.id)
      } yield {
        assert(response.code)(equalTo(StatusCode.InternalServerError)) &&
          assert(response.body.swap.toOption)(isSome(equalTo("message")))
      }
    }
      .provide(
        stubPendingUpdateRepositoryThatThrowsException,
        fixture,
      ),
  )
  
  private val fixture = ZLayer.makeSome[PendingUpdateRepository, DeviceApi](
    stubDeviceRepository,
    DeviceApi.layer,
    GetDevice.layer,
    ListDevices.layer,
    GetPendingUpdate.layer,
    GetPendingUpdates.layer,
    stubDeviceEventProducer,
  )
}