package is.valsk.esper.api

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.DeviceManufacturerHandler
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.domain
import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model, Name, UrlString}
import is.valsk.esper.domain.*
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.*
import is.valsk.esper.services.{EmailService, FirmwareDownloader}
import zio.mock.Mock
import zio.{IO, Ref, Task, ULayer, URIO, URLayer, ZIO, ZLayer, mock}

import java.io.IOException

trait ApiSpec {

  protected val nonExistentDeviceId: DeviceId = DeviceId("non-existent-device-id")

  val manufacturer1: Manufacturer = Manufacturer("test-device-1")
  val otherManufacturer: Manufacturer = Manufacturer("otherManufacturer")
  val manufacturerWithFailingHandler: Manufacturer = Manufacturer("failing-manufacturer")
  val unsupportedManufacturer: Manufacturer = Manufacturer("unsupported-manufacturer")
  val model1: NonEmptyString = Model("model1")
  val otherModel: NonEmptyString = Model("other-model")
  protected val device1: Device = domain.Device(
    id = DeviceId("id"),
    url = UrlString("https://fake.url"),
    name = Name("name"),
    nameByUser = Some("nameByUser"),
    model = model1,
    softwareVersion = Some(Version("softwareVersion")),
    manufacturer = manufacturer1,
  )
  protected val pendingUpdate1: PendingUpdate = PendingUpdate(
    device = device1,
    version = Version("version"),
  )

  val stubDeviceRepository: ULayer[DeviceRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[DeviceId, Device])
    } yield InMemoryDeviceRepository(ref)
  }

  class FirmwareDownloaderProbe(db: Ref[Seq[FirmwareDescriptor]]) extends FirmwareDownloader {
    override def downloadFirmware(manufacturer: Manufacturer, model: Model, version: Version)(using manufacturerHandler: DeviceManufacturerHandler): IO[EsperError, Firmware] = for {
      descriptor <- manufacturerHandler.getFirmwareDownloadDetails(manufacturer, model, Some(version))
      result <- downloadFirmware(descriptor)
    } yield result

    override def downloadFirmware(firmwareDescriptor: FirmwareDescriptor): IO[EsperError, Firmware] = for {
      _ <- db.update(_ :+ firmwareDescriptor)
    } yield Firmware(
      manufacturer = firmwareDescriptor.manufacturer,
      model = firmwareDescriptor.model,
      version = firmwareDescriptor.version,
      data = Array.emptyByteArray,
      size = 0
    )

    def probeInvocations: Ref[Seq[FirmwareDescriptor]] = db
  }

  class EmailServiceProbe(db: Ref[Seq[(String, String)]]) extends EmailService {
    override def sendEmail(subject: String, content: String): Task[Unit] = {
      db.update(_ :+ (subject, content))
    }
    def probeInvocations: Ref[Seq[(String, String)]] = db
  }

  val stubFirmwareDownloader: ULayer[FirmwareDownloaderProbe] = ZLayer {
    for {
      ref <- Ref.make(Seq.empty[FirmwareDescriptor])
    } yield FirmwareDownloaderProbe(ref)
  }

  val stubPendingUpdateRepository: ULayer[PendingUpdateRepository] = ZLayer {
    for {
      ref <- Ref.make(Map.empty[DeviceId, PendingUpdate])
    } yield InMemoryPendingUpdateRepository(ref)
  }

  val stubEmailService: ULayer[EmailServiceProbe] = ZLayer {
    for {
      ref <- Ref.make(Seq.empty[(String, String)])
    } yield EmailServiceProbe(ref)
  }

  val stubDeviceRepositoryThatThrowsException: ULayer[DeviceRepository] = ZLayer.succeed(
    new DeviceRepository {
      override def get(id: DeviceId): IO[PersistenceException, Device] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def getOpt(id: DeviceId): IO[PersistenceException, Option[Device]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def getAll: IO[PersistenceException, List[Device]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def add(device: Device): IO[PersistenceException, Device] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def update(device: Device): IO[PersistenceException, Device] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))
      
      override def delete(id: DeviceId): IO[PersistenceException, Unit] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))
    }
  )

  val stubPendingUpdateRepositoryThatThrowsException: ULayer[PendingUpdateRepository] = ZLayer.succeed(
    new PendingUpdateRepository {
      override def get(id: DeviceId): IO[PersistenceException, PendingUpdate] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def getOpt(id: DeviceId): IO[PersistenceException, Option[PendingUpdate]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def getAll: IO[PersistenceException, List[PendingUpdate]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def add(device: PendingUpdate): IO[PersistenceException, PendingUpdate] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def update(device: PendingUpdate): IO[PersistenceException, PendingUpdate] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))
      
      override def delete(id: DeviceId): IO[PersistenceException, Unit] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))
    }
  )

  val stubFirmwareRepositoryThatThrowsException: ULayer[FirmwareRepository] = ZLayer.succeed(
    new FirmwareRepository {
      override def get(id: FirmwareKey): IO[PersistenceException, Firmware] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def getOpt(id: FirmwareKey): IO[PersistenceException, Option[Firmware]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def getAll: IO[PersistenceException, List[Firmware]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def add(firmware: Firmware): IO[PersistenceException, Firmware] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def update(firmware: Firmware): IO[PersistenceException, Firmware] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def delete(id: FirmwareKey): IO[PersistenceException, Unit] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def getLatestFirmware(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): IO[PersistenceException, Option[Firmware]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))

      override def listVersions(manufacturer: Manufacturer, model: Model)(using ordering: Ordering[Version]): IO[PersistenceException, List[Version]] = ZIO.fail(PersistenceException("message", Some(IOException("test"))))
    }
  )

  def givenFirmwares(firmwares: Firmware*): URIO[FirmwareRepository, Unit] = for {
    firmwareRepository <- ZIO.service[FirmwareRepository]
    _ <- ZIO.foreach(firmwares)(firmwareRepository.add).orDie
  } yield ()

  def givenDevices(devices: Device*): URIO[DeviceRepository, Unit] = for {
    deviceRepository <- ZIO.service[DeviceRepository]
    _ <- ZIO.foreach(devices)(deviceRepository.add).orDie
  } yield ()

  def givenPendingUpdates(pendingUpdates: PendingUpdate*): URIO[PendingUpdateRepository, Unit] = for {
    pendingUpdateRepository <- ZIO.service[PendingUpdateRepository]
    _ <- ZIO.foreach(pendingUpdates)(pendingUpdateRepository.add).orDie
  } yield ()

  object MockEmailService extends Mock[EmailService] {
    object SendEmail extends Effect[(String, String), Throwable, Unit]

    val compose: URLayer[mock.Proxy, EmailService] =
      ZLayer {
        for {
          proxy <- ZIO.service[mock.Proxy]
        } yield new EmailService {
          override def sendEmail(subject: String, content: String): Task[Unit] = proxy(SendEmail, subject, content)
        }
      }
  }
}
