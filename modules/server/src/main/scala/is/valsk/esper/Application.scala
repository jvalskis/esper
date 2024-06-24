package is.valsk.esper

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import is.valsk.esper.api.{ApiServerApp, HttpApi}
import is.valsk.esper.api.devices.DeviceApi
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.api.firmware.FirmwareApi
import is.valsk.esper.api.firmware.endpoints.*
import is.valsk.esper.api.ota.OtaApi
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.device.shelly.ShellyDeviceHandler
import is.valsk.esper.device.{DeviceHandler, DeviceManufacturerHandler, DeviceProxyRegistry}
import is.valsk.esper.domain.DeviceStatus.UpdateStatus
import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model, Name, UrlString}
import is.valsk.esper.domain.{Device, DeviceApiError, DeviceStatus, Firmware, FirmwareDownloadError, FlashResult, MalformedVersion, SemanticVersion, Version}
import is.valsk.esper.hass.messages.{HassResponseMessageParser, MessageIdGenerator, MessageParser, SequentialMessageIdGenerator}
import is.valsk.esper.hass.protocol.api.{AuthenticationHandler, ConnectHandler, HassResponseMessageHandler, ResultHandler}
import is.valsk.esper.hass.protocol.{ChannelHandler, ProtocolHandler, TextHandler, UnhandledMessageHandler}
import is.valsk.esper.hass.HassWebsocketApp
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.repositories.*
import is.valsk.esper.services.*
import zio.*
import zio.logging.backend.SLF4J
import zio.stream.ZStream
import zio.config.typesafe.FromConfigSourceTypesafe

object Application extends ZIOAppDefault {

  override val bootstrap: URLayer[Any, Unit] = Runtime.removeDefaultLoggers >>> Runtime.setConfigProvider(ConfigProvider.fromResourcePath()) >>> SLF4J.slf4j

  def program: ZIO[ManufacturerRepository & DeviceRepository & HassWebsocketApp & ApiServerApp & LatestFirmwareMonitorApp & FlywayService, Throwable, Unit] = for {
    _ <- ZIO.logInfo("Starting application...")
    _ <- fillDummyData
    _ <- runMigrations
    _ <- startApplication
  } yield ()

  private def fillDummyData: ZIO[ManufacturerRepository & DeviceRepository, Throwable, Unit] = for {
    manufacturerRepository <- ZIO.service[ManufacturerRepository]
    deviceRepository <- ZIO.service[DeviceRepository]
    _ <- deviceRepository.add(Device(
      id = DeviceId("10001"),
      url = UrlString("http://localhost/iot/acme-door-sensor-3000-1"),
      manufacturer = Manufacturer("ACME"),
      model = Model("WS5000"),
      name = Name("Window Sensor 5000"),
      nameByUser = Some("Small window"),
      softwareVersion = Some(Version("version1"))
    ))
    _ <- deviceRepository.add(Device(
      id = DeviceId("10002"),
      url = UrlString("http://localhost/iot/acme-door-sensor-3000-2"),
      manufacturer = Manufacturer("ACME"),
      model = Model("DS3000"),
      name = Name("Door Sensor 3000"),
      nameByUser = Some("Big door"),
      softwareVersion = Some(Version("version10"))
    ))
    _ <- deviceRepository.add(Device(
      id = DeviceId("10003"),
      url = UrlString("http://localhost/iot/acme-door-sensor-3000-3"),
      manufacturer = Manufacturer("ACME"),
      model = Model("DS3000"),
      name = Name("Door Sensor 3000"),
      nameByUser = None,
      softwareVersion = None
    ))
    _ <- deviceRepository.add(Device(
      id = DeviceId("sh-100001"),
      url = UrlString("http://localhost/iot/shelly-dw-1"),
      manufacturer = Manufacturer("Shelly"),
      model = Model("SHDW-2"),
      name = Name("Shelly Door/Window sensor"),
      nameByUser = Some("Bedroom / Window"),
      softwareVersion = None
    ))
    _ <- manufacturerRepository.add(new DeviceHandler {
      override def supportedManufacturer: Manufacturer = Manufacturer("ACME")

      override def getFirmwareDownloadDetails(manufacturer: Manufacturer, model: Model, version: Option[Version]): IO[FirmwareDownloadError, DeviceManufacturerHandler.FirmwareDescriptor] = ZIO.succeed(FirmwareDescriptor(
        manufacturer = manufacturer,
        model = model,
        version = Version("version2"),
        url = UrlString("http://localhost/acme-firmware"),
      ))

      override def versionOrdering: Ordering[Version] = SemanticVersion.Ordering

      override def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, Version] = ZIO.succeed(Version("version1"))

      override def flashFirmware(device: Device, firmware: Firmware): IO[DeviceApiError, FlashResult] = ZIO.succeed(
        FlashResult(
          previousVersion = Version("version1"),
          currentVersion = Version("version2"),
          updateStatus = UpdateStatus.done)
      )

      override def getDeviceStatus(device: Device): IO[DeviceApiError, DeviceStatus] = ZIO.succeed(DeviceStatus(UpdateStatus.done))

      override def restartDevice(device: Device): IO[DeviceApiError, Unit] = ZIO.unit

      override def toDomain(hassDevice: HassResult): IO[MalformedVersion | MessageParser.ParseError, Device] = ???

      override def parseVersion(version: String): Either[MalformedVersion, Version] = Right(Version(version))
    })
  } yield ()

  private def runMigrations: RIO[FlywayService, Unit] = for {
    flywayService <- ZIO.service[FlywayService]
    _ <- flywayService.runMigrations().catchSome {
      case e => ZIO.logError(s"Failed to run migrations: $e") *> flywayService.runRepairs() *> flywayService.runMigrations()
    }
  } yield ()

  private def startApplication: RIO[HassWebsocketApp & ApiServerApp & LatestFirmwareMonitorApp, Unit] = for {
    hassWebsocketApp <- ZIO.service[HassWebsocketApp]
    apiServerApp <- ZIO.service[ApiServerApp]
    periodicLatestFirmwareDownloadApp <- ZIO.service[LatestFirmwareMonitorApp]
    _ <- ZStream
      .mergeAllUnbounded(16)(
        ZStream.fromZIO(hassWebsocketApp.run).retry(Schedule.fixed(1000.seconds)),
        ZStream.fromZIO(apiServerApp.run),
        ZStream.fromZIO(periodicLatestFirmwareDownloadApp.run),
      )
      .runDrain
  } yield ()

  private val hassResponseMessageHandlerLayer: URLayer[AuthenticationHandler & ConnectHandler & ResultHandler, List[HassResponseMessageHandler]] = ZLayer {
    for {
      authenticationHandler <- ZIO.service[AuthenticationHandler]
      connectHandler <- ZIO.service[ConnectHandler]
      resultHandler <- ZIO.service[ResultHandler]
    } yield List(authenticationHandler, connectHandler, resultHandler)
  }

  private val channelHandlerLayer: URLayer[ProtocolHandler & TextHandler & UnhandledMessageHandler, List[ChannelHandler]] = ZLayer {
    for {
      protocolHandler <- ZIO.service[ProtocolHandler]
      textHandler <- ZIO.service[TextHandler]
      unhandledMessageHandler <- ZIO.service[UnhandledMessageHandler]
    } yield List(protocolHandler, textHandler, unhandledMessageHandler)
  }

  private val manufacturerRegistryLayer: URLayer[ShellyDeviceHandler, Seq[DeviceHandler]] = ZLayer {
    for {
      shellyDeviceHandler <- ZIO.service[ShellyDeviceHandler]
    } yield Seq(
      shellyDeviceHandler
    )
  }

  private val quillPostgresLayer = Quill.DataSource.fromPrefix("esper.db") >>> Quill.Postgres.fromNamingStrategy(SnakeCase)

  override val run: URIO[Any, ExitCode] = ZIO.scoped(program)
    .provide(
      // Services
      FirmwareService.layer,
      OtaService.layer,
      PendingUpdateService.layer,
      FirmwareDownloader.layer,
      FlywayServiceLive.configuredLayer,
      EmailServiceLive.configuredLayer,

      // Repositories
      InMemoryDeviceRepository.layer,
      InMemoryManufacturerRepository.layer,
      FirmwareRepository.live,
      PendingUpdateRepository.live,

      // API
      ApiServerApp.configuredLayer,
      HttpApi.layer,
      // API - Firmware
      FirmwareApi.layer,
      GetFirmware.layer,
      ListFirmwareVersions.layer,
      DeleteFirmware.layer,
      DownloadFirmware.layer,
      DownloadLatestFirmware.layer,
      // API - Devices
      DeviceApi.layer,
      GetDevice.layer,
      ListDevices.layer,
      GetDeviceVersion.layer,
      FlashDevice.layer,
      // API - OTA
      OtaApi.layer,
      GetDeviceStatus.layer,
      RestartDevice.layer,
      GetPendingUpdates.layer,
      GetPendingUpdate.layer,

      // HASS
      HassWebsocketApp.configuredLayer,
      hassResponseMessageHandlerLayer,
      HassResponseMessageParser.layer,
      TextHandler.layer,
      ProtocolHandler.layer,
      UnhandledMessageHandler.layer,
      SequentialMessageIdGenerator.layer,
      AuthenticationHandler.configuredLayer,
      ConnectHandler.layer,
      ResultHandler.layer,
      channelHandlerLayer,

      // Firmware monitor
      LatestFirmwareMonitorApp.configuredLayer,

      // Devices
      DeviceProxyRegistry.layer,
      manufacturerRegistryLayer,
      ShellyDeviceHandler.configuredLayer,

      // Other
      HttpClient.configuredLayer,
      quillPostgresLayer,
    )
    .onError(cause =>
      val effect = if (cause.failures.exists(_.isInstanceOf[Config.Error])) {
        ZIO.logError(
          s"""Config error:
             |${cause.failures.distinct.mkString("\n")}
          """.stripMargin
        )
      } else {
        ZIO.logErrorCause("onError", cause)
      }
      effect.flatMap(_ => exit(ExitCode.failure))
    )
    .exitCode
}