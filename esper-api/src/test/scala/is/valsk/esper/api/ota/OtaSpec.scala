package is.valsk.esper.api.ota

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.device.DeviceStatus.UpdateStatus
import is.valsk.esper.device.{DeviceHandler, DeviceManufacturerHandler, DeviceStatus, FlashResult}
import is.valsk.esper.domain.Types.{DeviceId, Manufacturer, Model}
import is.valsk.esper.domain.*
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import zio.http.model.{HttpError, Method}
import zio.http.{Request, Response, URL}
import zio.{Exit, IO, ULayer, ZIO, ZLayer}

trait OtaSpec extends ApiSpec {

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

  def otaEndpoint: URL = {
    URL.fromString("/ota").toOption.get
  }

  def otaEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint ++ URL.fromString(s"/${deviceId.value}").toOption.get
  }

  def getDeviceVersionEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint(deviceId) ++ URL.fromString(s"/version").toOption.get
  }

  def getDeviceStatusEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint(deviceId) ++ URL.fromString(s"/status").toOption.get
  }

  def flashDeviceStatusEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint(deviceId) ++ URL.fromString(s"/flash").toOption.get
  }

  def flashDeviceStatusEndpoint(deviceId: DeviceId, version: Version): URL = {
    flashDeviceStatusEndpoint(deviceId) ++ URL.fromString(s"/${version.value}").toOption.get
  }

  def restartDeviceStatusEndpoint(deviceId: DeviceId): URL = {
    otaEndpoint(deviceId) ++ URL.fromString(s"/restart").toOption.get
  }

  def getDeviceVersion(deviceId: DeviceId): ZIO[OtaApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[OtaApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = getDeviceVersionEndpoint(deviceId))).exit
  } yield response

  def getDeviceStatus(deviceId: DeviceId): ZIO[OtaApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[OtaApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.POST, url = getDeviceStatusEndpoint(deviceId))).exit
  } yield response

  def flashDevice(deviceId: DeviceId): ZIO[OtaApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[OtaApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.POST, url = flashDeviceStatusEndpoint(deviceId))).exit
  } yield response

  def flashDevice(deviceId: DeviceId, version: Version): ZIO[OtaApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[OtaApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.POST, url = flashDeviceStatusEndpoint(deviceId, version))).exit
  } yield response

  def restartDevice(deviceId: DeviceId): ZIO[OtaApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[OtaApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.POST, url = restartDeviceStatusEndpoint(deviceId))).exit
  } yield response

}
