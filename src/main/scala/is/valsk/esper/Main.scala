package is.valsk.esper

import is.valsk.esper.api.ApiServerApp
import is.valsk.esper.device.shelly.{ShellyConfig, ShellyDevice}
import is.valsk.esper.types.Manufacturer
import is.valsk.esper.hass.device.{DeviceManufacturerHandler, InMemoryManufacturerRegistry}
import is.valsk.esper.hass.messages.{HassResponseMessageParser, MessageIdGenerator, SequentialMessageIdGenerator}
import is.valsk.esper.hass.protocol.api.{AuthenticationHandler, ConnectHandler, HassResponseMessageHandler, ResultHandler}
import is.valsk.esper.hass.protocol.{ChannelHandler, ProtocolHandler, TextHandler, UnhandledMessageHandler}
import is.valsk.esper.hass.{HassWebsocketApp, HassWebsocketClient, HassWebsocketClientImpl}
import is.valsk.esper.http.HttpClient
import is.valsk.esper.model.Device
import is.valsk.esper.services.{DeviceRepository, FirmwareDownloaderImpl, InMemoryDeviceRepository, ScheduleService}
import zio.*
import zio.config.ReadError
import zio.http.*
import zio.stream.ZStream

object Main extends ZIOAppDefault {

  private val scopedApp: ZIO[HassWebsocketApp & ApiServerApp, Throwable, Unit] = for {
    hassWebsockerApp <- ZIO.service[HassWebsocketApp]
    apiServerApp <- ZIO.service[ApiServerApp]
    _ <- ZStream
      .mergeAllUnbounded(16)(
        ZStream.fromZIO(hassWebsockerApp.run),
        ZStream.fromZIO(apiServerApp.run)
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

  private val manufacturerRegistryLayer: URLayer[ShellyDevice, Map[Manufacturer, DeviceManufacturerHandler]] = ZLayer {
    for {
      shellyDevice <- ZIO.service[ShellyDevice]
    } yield Map(
      Manufacturer.unsafeFrom("Shelly") -> shellyDevice
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
        HassWebsocketClientImpl.layer,
        HassWebsocketApp.layer,
        hassResponseMessageHandlerLayer,
        HassResponseMessageParser.layer,
        TextHandler.layer,
        ProtocolHandler.layer,
        UnhandledMessageHandler.layer,
        InMemoryManufacturerRegistry.layer,
        manufacturerRegistryLayer,
        ShellyConfig.layer,
        ShellyDevice.layer,
        HttpClient.layer,
        Client.default,
        FirmwareDownloaderImpl.layer,
      )
      .logError("Failed to start the application")
      .exitCode
  } yield x
}