package is.valsk.esper.api.firmware

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.device.{DeviceHandler, DeviceManufacturerHandler, DeviceStatus, FlashResult}
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import zio.{Exit, IO, ULayer, ZIO, ZLayer}
import zio.http.{Request, Response, URL}
import zio.http.model.{HttpError, Method}

trait FirmwareSpec extends ApiSpec {
  val firmwareDescriptor: FirmwareDescriptor = FirmwareDescriptor(
    manufacturer = manufacturer1,
    model = model1,
    version = Version("version2"),
    url = UrlString.unsafeFrom("http://localhost")
  )
  val stubManufacturerRegistryLayer: ULayer[Seq[DeviceHandler]] = ZLayer.succeed(
    Seq(
      new DeviceHandler {
        override def supportedManufacturer: Manufacturer = manufacturer1

        override def toDomain(hassDevice: HassResult): IO[MalformedVersion | ParseError, Device] = ???

        override def parseVersion(version: String): Either[MalformedVersion, Version] = ???

        override def getFirmwareDownloadDetails(manufacturer: Manufacturer, model: Model, version: Option[Version]): IO[FirmwareDownloadError, DeviceManufacturerHandler.FirmwareDescriptor] = {
          ZIO.succeed(
            firmwareDescriptor
          )
        }

        override def versionOrdering: Ordering[Version] = SemanticVersion.Ordering

        override def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, Version] = ???

        override def flashFirmware(device: Device, firmware: Firmware): IO[DeviceApiError, FlashResult] = ???

        override def getDeviceStatus(device: Device): IO[DeviceApiError, DeviceStatus] = ???

        override def restartDevice(device: Device): IO[DeviceApiError, Unit] = ???
      }
    )
  )

  def listFirmwares(manufacturer: Manufacturer, model: Model): ZIO[FirmwareApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[FirmwareApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = listFirmwaresEndpoint(manufacturer, model))).exit
  } yield response

  def getFirmware(manufacturer: Manufacturer, model: Model): ZIO[FirmwareApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[FirmwareApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = getFirmwareEndpoint(manufacturer, model))).exit
  } yield response

  def getFirmware(manufacturer: Manufacturer, model: Model, version: Version): ZIO[FirmwareApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[FirmwareApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.GET, url = getFirmwareEndpoint(manufacturer, model, version))).exit
  } yield response

  def downloadLatestFirmware(manufacturer: Manufacturer, model: Model): ZIO[FirmwareApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[FirmwareApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.POST, url = getFirmwareEndpoint(manufacturer, model))).exit
  } yield response

  def downloadFirmware(manufacturer: Manufacturer, model: Model, version: Version): ZIO[FirmwareApi, Nothing, Exit[Option[HttpError], Response]] = for {
    deviceApi <- ZIO.service[FirmwareApi]
    response <- deviceApi.app.runZIO(Request.default(method = Method.POST, url = getFirmwareEndpoint(manufacturer, model, version))).exit
  } yield response

  def getFirmwareEndpoint(manufacturer: Manufacturer, model: Model): URL = {
    URL.fromString(s"/firmware/$manufacturer/$model").toOption.get
  }

  def getFirmwareEndpoint(manufacturer: Manufacturer, model: Model, version: Version): URL = {
    getFirmwareEndpoint(manufacturer, model) ++ URL.fromString(s"/${version.value}").toOption.get
  }

  def listFirmwaresEndpoint(manufacturer: Manufacturer, model: Model): URL = {
    getFirmwareEndpoint(manufacturer, model) ++ URL.fromString(s"/list").toOption.get
  }

}
