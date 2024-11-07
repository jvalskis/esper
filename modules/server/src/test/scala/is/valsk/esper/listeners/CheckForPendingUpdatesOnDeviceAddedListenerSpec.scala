package is.valsk.esper.listeners

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.api.devices.GetDeviceSpec.{stubDeviceEventProducer, test}
import is.valsk.esper.api.firmware.DownloadLatestFirmwareSpec.unsupportedManufacturer
import is.valsk.esper.api.ota.GetDeviceStatusSpec.{device1, givenDevices}
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.device.*
import is.valsk.esper.domain
import is.valsk.esper.domain.*
import is.valsk.esper.domain.DeviceStatus.UpdateStatus
import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model, Name, UrlString}
import is.valsk.esper.event.DeviceAdded
import is.valsk.esper.listeners.CheckForPendingUpdatesOnDeviceAddedListener.CheckForPendingUpdatesOnDeviceAddedListenerLive
import is.valsk.esper.repositories.{DeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, InMemoryPendingUpdateRepository, ManufacturerRepository}
import is.valsk.esper.services.{FirmwareDownloader, FirmwareService, OtaService, PendingUpdateService}
import sttp.model.StatusCode
import zio.*
import zio.json.*
import zio.test.*
import zio.test.Assertion.*

object CheckForPendingUpdatesOnDeviceAddedListenerSpec extends ZIOSpecDefault {

  def spec = suite("CheckForPendingUpdatesOnDeviceAddedListener")(
      test("Return a 404 (Not Found) if the device does not exist") {
        val manufacturer1 = Manufacturer("test-device-1")
        val manufacturer2 = Manufacturer("test-device-2")
        val model1 = Model("model1")
        val device1 = Device(
          id = DeviceId("id1"),
          url = UrlString("https://fake1.url"),
          name = Name("name1"),
          nameByUser = Some("nameByUser1"),
          model = model1,
          softwareVersion = Some(Version("softwareVersion1")),
          manufacturer = manufacturer1,
        )
        val device2 = Device(
          id = DeviceId("id2"),
          url = UrlString("https://fake2.url"),
          name = Name("name2"),
          nameByUser = Some("nameByUser2"),
          model = model1,
          softwareVersion = Some(Version("softwareVersion2")),
          manufacturer = manufacturer2,
        )
        for {
          _ <- givenDevices(device1, device2)
          service <- ZIO.service[CheckForPendingUpdatesOnDeviceAddedListenerLive]
          _ <- service.onDeviceEvent(DeviceAdded(
            device1
          ))
          repository <- ZIO.service[InMemoryPendingUpdateRepository]
          allDevices <- repository.getAll
        } yield {
          assert(response.code)(equalTo(StatusCode.NotFound)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("Not found")))
        }
      },

      test("Fail with 412 (Precondition Failed) when latest firmware is not found") {
        for {
          _ <- givenDevices(device1)
          response <- flashDevice(device1.id)
        } yield {
          //            assert(response.code)(equalTo(StatusCode.PreconditionFailed)) && // TODO fix status code
          assert(response.code)(equalTo(StatusCode.NotFound)) &&
            assert(response.body.swap.toOption)(isSome(equalTo("Latest firmware not found")))
        }
      },
      test("Flash the device with the latest firmware") {
        {
          for {
            _ <- givenFirmwares(Firmware(
              manufacturer = device1.manufacturer,
              model = device1.model,
              version = Version("currentVersion"),
              data = Array.emptyByteArray,
              size = 0,
            ))
            _ <- givenDevices(device1)
            response <- flashDevice(device1.id)
            result = response.body.toOption.flatMap(_.fromJson[FlashResult].toOption)
          } yield {
            assert(result)(isSome(equalTo(FlashResult(
              previousVersion = Version("previousVersion"),
              currentVersion = Version("currentVersion"),
              updateStatus = UpdateStatus.done
            ))))
          }
        }
      }
    ).provide(
      PendingUpdateService.layer,
      InMemoryPendingUpdateRepository.layer,
      InMemoryFirmwareRepository.layer,
      InMemoryManufacturerRepository.layer,
    )
  )
}