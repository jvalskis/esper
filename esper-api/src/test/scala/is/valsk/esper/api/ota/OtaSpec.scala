package is.valsk.esper.api.ota

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.device.DeviceStatus.UpdateStatus
import is.valsk.esper.device.{DeviceHandler, DeviceManufacturerHandler, DeviceStatus, FlashResult}
import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.domain.*
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import zio.{IO, ULayer, ZIO, ZLayer}

trait OtaSpec extends ApiSpec {

  val manufacturerRegistryLayer: ULayer[Map[String, DeviceHandler]] = ZLayer {
    val value = for {
      testDeviceHandler <- ZIO.service[TestDeviceHandler]
      testFailingTestDeviceHandlerHandler <- ZIO.service[FailingTestDeviceHandler]
    } yield Map(
      testDeviceHandler.supportedManufacturer.toString -> testDeviceHandler,
      testFailingTestDeviceHandlerHandler.supportedManufacturer.toString -> testFailingTestDeviceHandlerHandler
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
}
