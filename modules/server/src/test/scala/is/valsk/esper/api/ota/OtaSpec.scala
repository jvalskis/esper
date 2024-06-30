package is.valsk.esper.api.ota

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.device.{DeviceHandler, DeviceManufacturerHandler}
import is.valsk.esper.domain.*
import is.valsk.esper.domain.DeviceStatus.UpdateStatus
import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{Response, UriContext, basicRequest}
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.{IO, Task, ULayer, ZIO, ZLayer}

trait OtaSpec extends ApiSpec {

  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  val stubManufacturerRegistryLayer: ULayer[Seq[DeviceHandler]] = ZLayer {
    val value = for {
      testDeviceHandler <- ZIO.service[TestDeviceHandler]
      testFailingTestDeviceHandlerHandler <- ZIO.service[FailingTestDeviceHandler]
    } yield Seq(
      testDeviceHandler,
      testFailingTestDeviceHandlerHandler
    )
    value.provide(
      ZLayer.succeed(TestDeviceHandler()),
      ZLayer.succeed(FailingTestDeviceHandler()),
    )
  }

  case class TestDeviceHandler() extends DeviceHandler {
    override def getFirmwareDownloadDetails(
        manufacturer: Manufacturer,
        model: Model,
        version: Option[Version]
    ): IO[FirmwareDownloadError, DeviceManufacturerHandler.FirmwareDescriptor] = ???

    override def versionOrdering: Ordering[Version] = Ordering.String.on(_.value)

    override def toDomain(hassDevice: HassResult): IO[MalformedVersion | ParseError, Device] = ???

    override def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, Version] = ZIO.succeed(Version("currentFirmwareVersion"))

    override def flashFirmware(device: Device, firmware: Firmware): IO[DeviceApiError, FlashResult] = ZIO.succeed(FlashResult(
      previousVersion = Version("previousVersion"),
      currentVersion = firmware.version,
      updateStatus = UpdateStatus.done
    ))

    override def getDeviceStatus(device: Device): IO[DeviceApiError, DeviceStatus] = ZIO.succeed(DeviceStatus(UpdateStatus.done))

    override def restartDevice(device: Device): IO[DeviceApiError, Unit] = ???

    override def parseVersion(version: String): Either[MalformedVersion, Version] = ???

    def supportedManufacturer: Manufacturer = manufacturer1
  }

  case class FailingTestDeviceHandler() extends DeviceHandler {
    override def getFirmwareDownloadDetails(
        manufacturer: Manufacturer,
        model: Model,
        version: Option[Version]
    ): IO[FirmwareDownloadError, DeviceManufacturerHandler.FirmwareDescriptor] = ???

    override def versionOrdering: Ordering[Version] = Ordering.String.on(_.value)

    override def toDomain(hassDevice: HassResult): IO[MalformedVersion | ParseError, Device] = ???

    override def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, Version] = ZIO.fail(ApiCallFailed("error", device))

    override def flashFirmware(device: Device, firmware: Firmware): IO[DeviceApiError, FlashResult] = ZIO.fail(ApiCallFailed("error", device))

    override def getDeviceStatus(device: Device): IO[DeviceApiError, DeviceStatus] = ZIO.fail(ApiCallFailed("error", device))

    override def restartDevice(device: Device): IO[DeviceApiError, Unit] = ZIO.fail(ApiCallFailed("error", device))

    override def parseVersion(version: String): Either[MalformedVersion, Version] = ???

    def supportedManufacturer: Manufacturer = manufacturerWithFailingHandler
  }

  def getDeviceVersion(deviceId: DeviceId): ZIO[OtaApi, Throwable, Response[Either[String, String]]] = for {
    otaApi <- ZIO.service[OtaApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(otaApi.getDeviceVersionEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .get(uri"/api/ota/$deviceId/version")
      .send(backendStub)
  } yield response

  def getDeviceStatus(deviceId: DeviceId): ZIO[OtaApi, Throwable, Response[Either[String, String]]] = for {
    otaApi <- ZIO.service[OtaApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(otaApi.getDeviceStatusEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .get(uri"/api/ota/$deviceId/status")
      .send(backendStub)
  } yield response

  def flashDevice(deviceId: DeviceId): ZIO[OtaApi, Throwable, Response[Either[String, String]]] = for {
    otaApi <- ZIO.service[OtaApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(otaApi.flashDeviceWithLatestVersionEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .post(uri"/api/ota/$deviceId/flash")
      .send(backendStub)
  } yield response

  def flashDevice(deviceId: DeviceId, version: Version): ZIO[OtaApi, Throwable, Response[Either[String, String]]] = for {
    otaApi <- ZIO.service[OtaApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(otaApi.flashDeviceEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .post(uri"/api/ota/$deviceId/flash/${version.value}")
      .send(backendStub)
  } yield response

  def restartDevice(deviceId: DeviceId): ZIO[OtaApi, Throwable, Response[Either[String, String]]] = for {
    otaApi <- ZIO.service[OtaApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(otaApi.restartDeviceEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .post(uri"/api/ota/$deviceId/restart")
      .send(backendStub)
  } yield response

}
