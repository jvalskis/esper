package is.valsk.esper.api.devices

import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.ctx.DeviceCtx
import is.valsk.esper.domain.Device
import is.valsk.esper.repositories.{DeviceRepository, InMemoryPendingUpdateRepository}
import sttp.model.StatusCode
import zio.*
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object ListDevicesSpec extends ZIOSpecDefault with DevicesSpec with DeviceCtx {

  def spec = suite("ListDevicesSpec")(
    suite("Normal flow")(
      test("Return an empty list if there are no devices") {
        for {
          result <- invokeListDevices
          response <- invokeListDevices
          result = response.body.toSeq.flatMap(_.fromJson[List[Device]].toSeq).flatten
        } yield assert(result)(isEmpty)
      },
      test("Return all devices when there are some") {
        for {
          _ <- givenDevices(device1)
          response <- invokeListDevices
          result = response.body.toSeq.flatMap(_.fromJson[List[Device]].toSeq).flatten
        } yield {
          assert(result)(contains(device1))
        }
      }
    )
      .provide(
        stubDeviceRepository,
        DeviceApi.layer,
        GetDevice.layer,
        ListDevices.layer,
        GetPendingUpdate.layer,
        GetPendingUpdates.layer,
        InMemoryPendingUpdateRepository.layer,
        stubDeviceEventProducer,
      ),
    test("Fail with 500 (Internal Server Error) when there is an exception while fetching devices") {
      for {
        response <- invokeListDevices
      } yield {
        assert(response.code)(equalTo(StatusCode.InternalServerError)) &&
        assert(response.body.swap.toOption)(isSome(equalTo("message")))
      }
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