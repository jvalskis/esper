package is.valsk.esper

import io.getquill.context.Context
import io.getquill.idiom.Idiom
import io.getquill.jdbczio.Quill
import io.getquill.{MysqlJdbcContext, MysqlZioJdbcContext, NamingStrategy, PostgresDialect, PostgresJdbcContext, PostgresZioJdbcContext, Query, SnakeCase, SqliteJdbcContext, SqliteZioJdbcContext}
import is.valsk.esper.api.devices.{FlashDevice, GetDevice, GetDeviceVersion, GetDevices}
import is.valsk.esper.api.firmware.{DeleteFirmware, DownloadFirmware, DownloadLatestFirmware, GetFirmware, GetLatestFirmware, ListFirmwareVersions}
import is.valsk.esper.api.{ApiServerApp, DeviceApi, FirmwareApi}
import is.valsk.esper.device.shelly.{ShellyConfig, ShellyDeviceHandler}
import is.valsk.esper.device.{DeviceManufacturerHandler, DeviceProxy, DeviceProxyRegistry}
import is.valsk.esper.domain.Types.Manufacturer
import is.valsk.esper.domain.{Device, Firmware, PersistenceException}
import is.valsk.esper.hass.messages.{HassResponseMessageParser, MessageIdGenerator, SequentialMessageIdGenerator}
import is.valsk.esper.hass.protocol.api.{AuthenticationHandler, ConnectHandler, HassResponseMessageHandler, ResultHandler}
import is.valsk.esper.hass.protocol.{ChannelHandler, ProtocolHandler, TextHandler, UnhandledMessageHandler}
import is.valsk.esper.hass.{HassToDomainMapper, HassWebsocketApp}
import is.valsk.esper.repositories.*
import is.valsk.esper.services.{FirmwareDownloader, HttpClient, LatestFirmwareMonitorApp}
import zio.*
import zio.config.ReadError
import zio.http.*
import zio.logging.backend.SLF4J
import zio.stream.ZStream

object Main extends ZIOAppDefault {

  override val bootstrap: URLayer[Any, Unit] = Runtime.removeDefaultLoggers >>> SLF4J.slf4j

  private val scopedApp: ZIO[HassWebsocketApp & ApiServerApp & LatestFirmwareMonitorApp, Throwable, Unit] = for {
    hassWebsockerApp <- ZIO.service[HassWebsocketApp]
    apiServerApp <- ZIO.service[ApiServerApp]
    periodicLatestFirmwareDownloadApp <- ZIO.service[LatestFirmwareMonitorApp]
    _ <- ZStream
      .mergeAllUnbounded(16)(
        ZStream.fromZIO(hassWebsockerApp.run),
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

  private val manufacturerRegistryLayer: URLayer[ShellyDeviceHandler, Map[Manufacturer, DeviceManufacturerHandler with HassToDomainMapper with DeviceProxy]] = ZLayer {
    for {
      shellyDeviceHandler <- ZIO.service[ShellyDeviceHandler]
    } yield Map(
      shellyDeviceHandler.supportedManufacturer -> shellyDeviceHandler
    )
  }

  private val quillPostgresLayer = Quill.DataSource.fromPrefix("PostgresConfig") >>> Quill.Postgres.fromNamingStrategy(SnakeCase)

  override val run: URIO[Any, ExitCode] = for {
    x <- ZIO.scoped(scopedApp)
      .provide(
        EsperConfig.layer,
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
        GetLatestFirmware.layer,
        DeleteFirmware.layer,
        DownloadFirmware.layer,
        DownloadLatestFirmware.layer,
        GetDevice.layer,
        GetDevices.layer,
        GetDeviceVersion.layer,
        FlashDevice.layer,
        DeviceProxyRegistry.layer,
        quillPostgresLayer,
        FirmwareRepository.live,
      )
      .logError("Failed to start the application")
      .exitCode
  } yield x
}