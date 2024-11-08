package is.valsk.esper.listeners

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.api.devices.GetDeviceSpec.test
import is.valsk.esper.api.ota.GetDeviceVersionSpec.stubManufacturerRegistryLayer
import is.valsk.esper.ctx.{DeviceCtx, FirmwareCtx, PendingUpdateCtx}
import is.valsk.esper.device.*
import is.valsk.esper.domain
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.*
import is.valsk.esper.event.FirmwareDownloaded
import is.valsk.esper.listeners.CheckForPendingUpdatesOnFirmwareAddedListener.CheckForPendingUpdatesOnFirmwareAddedListenerLive
import is.valsk.esper.listeners.CheckForPendingUpdatesOnFirmwareAddedListenerSpec.Ctx
import is.valsk.esper.repositories.*
import is.valsk.esper.services.PendingUpdateService
import zio.*
import zio.test.*
import zio.test.Assertion.*

object CheckForPendingUpdatesOnFirmwareAddedListenerSpec extends ZIOSpecDefault with ApiSpec with Ctx {

  def spec: Spec[Any, Throwable] =
    suite("CheckForPendingUpdatesOnFirmwareAddedListener")(
      test("Insert pending update for all devices with older versions when new Firmware with latest version was added") {
        for {
          _ <- givenFirmwares(
            oldFirmware,
            latestFirmware,
          )
          devices <- givenDevicesZIO(
            device(manufacturer1, model1, oldVersion),
            device(manufacturer1, model1, oldVersion),
          )
          outdatedDevice1 +: outdatedDevice2 +: _ = devices: @unchecked
          _ <- onFirmwareAdded(latestFirmware)
          pendingUpdates <- getAllPendingUpdates
        } yield {
          assert(pendingUpdates)(equalTo(Seq(
            PendingUpdate(
              device = outdatedDevice1,
              version = latestVersion,
            ),
            PendingUpdate(
              device = outdatedDevice2,
              version = latestVersion,
            ),
          )))
        }
      },

      test("Ignore devices with mismatching model and manufacturer when new Firmware with latest version was added") {
        for {
          _ <- givenFirmwares(
            oldFirmware,
            latestFirmware,
          )
          _ <- givenDevicesZIO(
            device(manufacturer2, model1, oldVersion),
            device(manufacturer1, model2, oldVersion),
          )
          _ <- onFirmwareAdded(latestFirmware)
          pendingUpdate <- getAllPendingUpdates
        } yield {
          assert(pendingUpdate)(isEmpty)
        }
      },

      test("Ignore devices with never version when new Firmware was added") {
        for {
          _ <- givenFirmwares(
            oldFirmware,
            latestFirmware,
          )
          _ <- givenDevicesZIO(
            device(manufacturer1, model1, latestVersion)
          )
          _ <- onFirmwareAdded(latestFirmware)
          pendingUpdate <- getAllPendingUpdates
        } yield {
          assert(pendingUpdate)(isEmpty)
        }
      },

      test("Fail with ManufacturerNotSupported error when manufacturer is not supported") {
        val unsupportedDevice = device1.copy(manufacturer = unsupportedManufacturer)

        for {
          _ <- givenDevices(unsupportedDevice)
          exit <- onFirmwareAdded(latestFirmware.copy(manufacturer = unsupportedManufacturer)).exit
        } yield {
          assertTrue(exit == Exit.fail(ManufacturerNotSupported(unsupportedManufacturer)))
        }
      },

      test("Send email when latest firmware is downloaded and there are new pending updates") {
        for {
          _ <- givenFirmwares(
            latestFirmware,
          )
          devices <- givenDevicesZIO(
            device(manufacturer1, model1, oldVersion),
          )
          _ <- onFirmwareAdded(latestFirmware)
          emailServiceProbe <- ZIO.service[EmailServiceProbe]
          invocations <- emailServiceProbe.probeInvocations.get
        } yield {
          assert(invocations)(contains((
            "Esper: there are 1 pending updates",
            s"""
              |<p>The following devices have pending firmware updates:</p>
              |<ul>
              |<li>$manufacturer1 $model1 (${devices.head.id}): $latestVersion</li>
              |</ul>
              |""".stripMargin
          )))
        }
      },

      test("Send no email if latest firmware is downloaded but there are no new pending updates") {
        for {
          _ <- givenFirmwares(
            latestFirmware,
          )
          _ <- givenDevicesZIO(
            device(manufacturer1, model1, latestVersion),
          )
          _ <- onFirmwareAdded(latestFirmware)
          emailServiceProbe <- ZIO.service[EmailServiceProbe]
          invocations <- emailServiceProbe.probeInvocations.get
        } yield {
          assert(invocations)(isEmpty)
        }
      },
    ).provide(
      PendingUpdateService.layer,
      InMemoryPendingUpdateRepository.layer,
      InMemoryFirmwareRepository.layer,
      InMemoryManufacturerRepository.layer,
      CheckForPendingUpdatesOnFirmwareAddedListener.layer,
      stubManufacturerRegistryLayer,
      stubDeviceRepository,
      stubDeviceEventProducer,
      stubEmailService,
    )

  trait Ctx extends PendingUpdateCtx with FirmwareCtx with DeviceCtx {

    val manufacturer2: Manufacturer = Manufacturer("test-device-2")
    val model2: Model = Model("model2")
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
      manufacturer = manufacturer1,
      model = model1,
      version = latestVersion,
      data = Array.emptyByteArray,
      size = 0,
    )

    def device(manufacturer: Manufacturer, model: Model, softwareVersion: Version): UIO[Device] = for {
      id <- Random.nextInt
    } yield Device(
      id = DeviceId(s"${id.abs}"),
      url = UrlString("https://fake.url"),
      name = Name("name"),
      nameByUser = Some("nameByUser"),
      model = model,
      softwareVersion = Some(softwareVersion),
      manufacturer = manufacturer,
    )

    val device2: Device = Device(
      id = DeviceId("id2"),
      url = UrlString("https://fake.url"),
      name = Name("name"),
      nameByUser = Some("nameByUser"),
      model = model1,
      softwareVersion = Some(Version("softwareVersion")),
      manufacturer = manufacturer1,
    )

    def onFirmwareAdded(firmware: Firmware): ZIO[CheckForPendingUpdatesOnFirmwareAddedListenerLive, Throwable, Unit] =
      ZIO.service[CheckForPendingUpdatesOnFirmwareAddedListenerLive].flatMap(_.onFirmwareEvent(FirmwareDownloaded(firmware)))
  }

}