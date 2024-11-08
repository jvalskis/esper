package is.valsk.esper.listeners

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.firmware.DownloadLatestFirmwareSpec.{givenFirmwares, unsupportedManufacturer}
import is.valsk.esper.api.ota.FlashDeviceSpec.givenPendingUpdates
import is.valsk.esper.api.ota.GetDeviceVersionSpec.stubManufacturerRegistryLayer
import is.valsk.esper.device.*
import is.valsk.esper.domain
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.*
import is.valsk.esper.event.{DeviceAdded, DeviceEvent}
import is.valsk.esper.listeners.CheckForPendingUpdatesOnDeviceAddedListener.CheckForPendingUpdatesOnDeviceAddedListenerLive
import is.valsk.esper.listeners.CheckForPendingUpdatesOnDeviceAddedListenerSpec.Ctx
import is.valsk.esper.repositories.*
import is.valsk.esper.services.PendingUpdateService
import zio.*
import zio.test.*
import zio.test.Assertion.*

object CheckForPendingUpdatesOnDeviceAddedListenerSpec extends ZIOSpecDefault with ApiSpec with Ctx {

  def spec: Spec[Any, Throwable] =
    suite("CheckForPendingUpdatesOnDeviceAddedListener")(
      test("Insert pending update when new Device has an outdated firmware version") {
        for {
          _ <- givenFirmwares(
            oldFirmware,
            latestFirmware,
          )
          deviceWithOldVersion = device1.copy(softwareVersion = Some(oldVersion))
          _ <- onDeviceEvent(DeviceAdded(
            deviceWithOldVersion
          ))
          repository <- ZIO.service[PendingUpdateRepository]
          pendingUpdate <- repository.get(device1.id)
        } yield {
          assert(pendingUpdate)(equalTo(PendingUpdate(
            device = deviceWithOldVersion,
            version = latestVersion,
          )))
        }
      },

      test("Do nothing when new Device has an outdated firmware version but there are no firmwares downloaded") {
        for {
          _ <- onDeviceEvent(DeviceAdded(
            device1.copy(softwareVersion = Some(oldVersion))
          ))
          repository <- ZIO.service[PendingUpdateRepository]
          maybePendingUpdate <- repository.getOpt(device1.id)
        } yield {
          assert(maybePendingUpdate)(isNone)
        }
      },

      test("Overwrite existing pending update when Device with an outdated firmware version was added again") {
        for {
          _ <- givenFirmwares(
            latestFirmware,
          )
          _ <- givenPendingUpdates(
            PendingUpdate(
              device = device1,
              version = oldVersion,
            )
          )
          _ <- onDeviceEvent(DeviceAdded(
            device1.copy(softwareVersion = Some(oldVersion))
          ))
          repository <- ZIO.service[PendingUpdateRepository]
          pendingUpdate <- repository.get(device1.id)
        } yield {
          assert(pendingUpdate)(equalTo(PendingUpdate(
            device = device1,
            version = latestVersion,
          )))
        }
      },

      test("Fail with ManufacturerNotSupported error when manufacturer is not supported") {
        val unsupportedDevice = device1.copy(manufacturer = unsupportedManufacturer)

        for {
          exit <- onDeviceEvent(DeviceAdded(unsupportedDevice)).exit
        } yield {
          assertTrue(exit == Exit.fail(ManufacturerNotSupported(unsupportedManufacturer)))
        }
      }
    ).provide(
      PendingUpdateService.layer,
      InMemoryPendingUpdateRepository.layer,
      InMemoryFirmwareRepository.layer,
      InMemoryManufacturerRepository.layer,
      CheckForPendingUpdatesOnDeviceAddedListener.layer,
      stubManufacturerRegistryLayer,
    )

  trait Ctx {

    val oldVersion: Version = Version("1.0.0")
    val oldFirmware: Firmware = Firmware(
      manufacturer = device1.manufacturer,
      model = device1.model,
      version = oldVersion,
      data = Array.emptyByteArray,
      size = 0,
    )
    val latestVersion: Version = Version("1.2.3")
    val latestFirmware: Firmware = Firmware(
      manufacturer = device1.manufacturer,
      model = device1.model,
      version = latestVersion,
      data = Array.emptyByteArray,
      size = 0,
    )

    def onDeviceEvent(event: DeviceEvent): ZIO[CheckForPendingUpdatesOnDeviceAddedListenerLive, Throwable, Unit] =
      ZIO.service[CheckForPendingUpdatesOnDeviceAddedListenerLive].flatMap(_.onDeviceEvent(event))

  }
}