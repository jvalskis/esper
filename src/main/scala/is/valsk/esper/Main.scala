package is.valsk.esper

import is.valsk.esper.api.devices.{FlashDevice, GetDevice, GetDeviceVersion, GetDevices}
import is.valsk.esper.api.firmware.{DeleteFirmware, DownloadFirmware, GetFirmware}
import is.valsk.esper.api.{ApiServerApp, DeviceApi, FirmwareApi}
import is.valsk.esper.device.DeviceManufacturerHandler
import is.valsk.esper.device.shelly.{ShellyConfig, ShellyDeviceHandler}
import is.valsk.esper.domain.Device
import is.valsk.esper.domain.Types.Manufacturer
import is.valsk.esper.hass.messages.{HassResponseMessageParser, MessageIdGenerator, SequentialMessageIdGenerator}
import is.valsk.esper.hass.protocol.api.{AuthenticationHandler, ConnectHandler, HassResponseMessageHandler, ResultHandler}
import is.valsk.esper.hass.protocol.{ChannelHandler, ProtocolHandler, TextHandler, UnhandledMessageHandler}
import is.valsk.esper.hass.{HassToDomainMapper, HassWebsocketApp}
import is.valsk.esper.repositories.{InMemoryDeviceRepository, InMemoryFirmwareRepository, InMemoryManufacturerRepository, Repository}
import is.valsk.esper.services.{FirmwareDownloader, HttpClient, LatestFirmwareMonitorApp}
import zio.*
import zio.config.ReadError
import zio.http.*
import zio.stream.ZStream

object Main extends ZIOAppDefault {

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

  private val manufacturerRegistryLayer: URLayer[ShellyDeviceHandler, Map[Manufacturer, DeviceManufacturerHandler with HassToDomainMapper]] = ZLayer {
    for {
      shellyDeviceHandler <- ZIO.service[ShellyDeviceHandler]
    } yield Map(
      Manufacturer.unsafeFrom("Shelly") -> shellyDeviceHandler
    )
  }

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
        InMemoryFirmwareRepository.layer,
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
        DeleteFirmware.layer,
        DownloadFirmware.layer,
        GetDevice.layer,
        GetDevices.layer,
        GetDeviceVersion.layer,
        FlashDevice.layer,
      )
      .logError("Failed to start the application")
      .exitCode
  } yield x
}