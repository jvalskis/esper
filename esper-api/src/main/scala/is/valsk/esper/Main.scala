package is.valsk.esper

import io.getquill.SnakeCase
import io.getquill.jdbczio.Quill
import is.valsk.esper.api.ApiServerApp
import is.valsk.esper.api.devices.DeviceApi
import is.valsk.esper.api.devices.endpoints.{GetDevice, GetPendingUpdate, GetPendingUpdates, ListDevices}
import is.valsk.esper.api.firmware.FirmwareApi
import is.valsk.esper.api.firmware.endpoints.*
import is.valsk.esper.api.ota.OtaApi
import is.valsk.esper.api.ota.endpoints.{FlashDevice, GetDeviceStatus, GetDeviceVersion, RestartDevice}
import is.valsk.esper.device.shelly.ShellyDeviceHandler
import is.valsk.esper.device.{DeviceHandler, DeviceProxyRegistry}
import is.valsk.esper.hass.messages.{HassResponseMessageParser, MessageIdGenerator, SequentialMessageIdGenerator}
import is.valsk.esper.hass.protocol.api.{AuthenticationHandler, ConnectHandler, HassResponseMessageHandler, ResultHandler}
import is.valsk.esper.hass.protocol.{ChannelHandler, ProtocolHandler, TextHandler, UnhandledMessageHandler}
import is.valsk.esper.hass.HassWebsocketApp
import is.valsk.esper.repositories.*
import is.valsk.esper.services.*
import zio.*
import zio.logging.backend.SLF4J
import zio.stream.ZStream
import zio.config.typesafe.FromConfigSourceTypesafe

object Main extends ZIOAppDefault {

  override val bootstrap: URLayer[Any, Unit] = Runtime.removeDefaultLoggers >>> Runtime.setConfigProvider(ConfigProvider.fromResourcePath()) >>> SLF4J.slf4j

  def program: ZIO[HassWebsocketApp & ApiServerApp & LatestFirmwareMonitorApp & FlywayService, Throwable, Unit] = for {
    _ <- ZIO.logInfo("Starting application...")
    _ <- runMigrations
    _ <- startApplication
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
        ZStream.fromZIO(hassWebsocketApp.run).retry(Schedule.fixed(10.seconds)),
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