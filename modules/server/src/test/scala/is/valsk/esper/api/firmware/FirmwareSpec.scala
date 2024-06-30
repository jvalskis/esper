package is.valsk.esper.api.firmware

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.device.{DeviceHandler, DeviceManufacturerHandler}
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{Response, UriContext, basicRequest}
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.{IO, Task, ULayer, ZIO, ZLayer}

trait FirmwareSpec extends ApiSpec {

  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

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

  def listFirmwareVersions(manufacturer: Manufacturer, model: Model): ZIO[FirmwareApi, Throwable, Response[Either[String, String]]] = for {
    firmwareApi <- ZIO.service[FirmwareApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(firmwareApi.listFirmwareVersionsEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .get(uri"/api/firmware/$manufacturer/$model/list")
      .send(backendStub)
  } yield response

  def getFirmware(manufacturer: Manufacturer, model: Model): ZIO[FirmwareApi, Throwable, Response[Either[String, String]]] = for {
    firmwareApi <- ZIO.service[FirmwareApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(firmwareApi.getLatestFirmwareEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .get(uri"/api/firmware/$manufacturer/$model")
      .send(backendStub)
  } yield response

  def getFirmware(manufacturer: Manufacturer, model: Model, version: Version): ZIO[FirmwareApi, Throwable, Response[Either[String, String]]] = for {
    firmwareApi <- ZIO.service[FirmwareApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(firmwareApi.getFirmwareEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .get(uri"/api/firmware/$manufacturer/$model/${version.value}")
      .send(backendStub)
  } yield response

  def downloadLatestFirmware(manufacturer: Manufacturer, model: Model): ZIO[FirmwareApi, Throwable, Response[Either[String, String]]] = for {
    firmwareApi <- ZIO.service[FirmwareApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(firmwareApi.downloadLatestFirmwareEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .post(uri"/api/firmware/$manufacturer/$model")
      .send(backendStub)
  } yield response

  def downloadFirmware(manufacturer: Manufacturer, model: Model, version: Version): ZIO[FirmwareApi, Throwable, Response[Either[String, String]]] = for {
    firmwareApi <- ZIO.service[FirmwareApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(firmwareApi.downloadFirmwareEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .post(uri"/api/firmware/$manufacturer/$model/${version.value}")
      .send(backendStub)
  } yield response

}
