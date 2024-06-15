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
import is.valsk.esper.device.shelly.{ShellyConfig, ShellyDeviceHandler}
import is.valsk.esper.device.{DeviceHandler, DeviceManufacturerHandler, DeviceProxy, DeviceProxyRegistry}
import is.valsk.esper.domain.Types.Manufacturer
import is.valsk.esper.hass.messages.{HassResponseMessageParser, MessageIdGenerator, SequentialMessageIdGenerator}
import is.valsk.esper.hass.protocol.api.{AuthenticationHandler, ConnectHandler, HassResponseMessageHandler, ResultHandler}
import is.valsk.esper.hass.protocol.{ChannelHandler, ProtocolHandler, TextHandler, UnhandledMessageHandler}
import is.valsk.esper.hass.{HassToDomainMapper, HassWebsocketApp}
import is.valsk.esper.repositories.*
import is.valsk.esper.services.*
import zio.*
import zio.Cause.{Die, Fail}
import zio.http.*
import zio.logging.backend.SLF4J
import zio.stream.ZStream
import zio.config.typesafe.FromConfigSourceTypesafe

object Main extends ZIOAppDefault {

  override val bootstrap: URLayer[Any, Unit] = Runtime.removeDefaultLoggers >>> Runtime.setConfigProvider(ConfigProvider.fromResourcePath()) >>> SLF4J.slf4j

  private val scopedApp: ZIO[HassWebsocketApp & ApiServerApp & LatestFirmwareMonitorApp, Throwable, Unit] = for {
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

  private val manufacturerRegistryLayer: URLayer[ShellyDeviceHandler, Map[Manufacturer, DeviceHandler]] = ZLayer {
    for {
      shellyDeviceHandler <- ZIO.service[ShellyDeviceHandler]
    } yield Map(
      shellyDeviceHandler.supportedManufacturer -> shellyDeviceHandler
    )
  }

  private val quillPostgresLayer = Quill.DataSource.fromPrefix("PostgresConfig") >>> Quill.Postgres.fromNamingStrategy(SnakeCase)

  override val run: URIO[Any, ExitCode] = ZIO.scoped(scopedApp)
    .provide(
      EsperConfig.layer,
      EsperConfig.scheduleConfigLayer,
      InMemoryDeviceRepository.layer,
      ApiServerApp.layer,
      SequentialMessageIdGenerator.layer,
      AuthenticationHandler.layer,
      ConnectHandler.layer,
      ResultHandler.layer,
      channelHandlerLayer,
      HassWebsocketApp.layer,
      hassResponseMessageHandlerLayer,
      HassResponseMessageParser.layer,
      TextHandler.layer,
      ProtocolHandler.layer,
      UnhandledMessageHandler.layer,
      InMemoryManufacturerRepository.layer,
      manufacturerRegistryLayer,
      ShellyConfig.layer,
      ShellyDeviceHandler.layer,
      HttpClient.layer,
      Client.default,
      FirmwareDownloader.layer,
      LatestFirmwareMonitorApp.layer,
      DeviceApi.layer,
      FirmwareApi.layer,
      GetFirmware.layer,
      ListFirmwareVersions.layer,
      FirmwareService.layer,
      DeleteFirmware.layer,
      DownloadFirmware.layer,
      DownloadLatestFirmware.layer,
      GetDevice.layer,
      ListDevices.layer,
      GetDeviceVersion.layer,
      FlashDevice.layer,
      DeviceProxyRegistry.layer,
      quillPostgresLayer,
      FirmwareRepository.live,
      OtaApi.layer,
      GetDeviceStatus.layer,
      OtaService.layer,
      RestartDevice.layer,
      GetPendingUpdates.layer,
      PendingUpdateService.layer,
      PendingUpdateRepository.live,
      GetPendingUpdate.layer,
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