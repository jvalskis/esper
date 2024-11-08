package is.valsk.esper.device.shelly

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.config.HttpServerConfig
import is.valsk.esper.device.DeviceHandler
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.device.shelly.ShellyDeviceHandler.{ApiEndpoints, ShellyFirmwareEntry}
import is.valsk.esper.device.shelly.api.Ota
import is.valsk.esper.device.shelly.api.Ota.decoder
import is.valsk.esper.domain.*
import is.valsk.esper.domain.DeviceStatus.UpdateStatus
import is.valsk.esper.domain.Types.{Manufacturer, Model, UrlString}
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.services.HttpClient
import zio.http.{Path, URL}
import zio.json.{DeriveJsonDecoder, JsonDecoder}
import zio.{Chunk, IO, RLayer, Schedule, URLayer, ZIO, ZLayer, durationInt}

class ShellyDeviceHandler(
    serverConfig: HttpServerConfig,
    shellyConfig: ShellyConfig,
    httpClient: HttpClient,
) extends DeviceHandler {

  private val shellyApiVersionPattern = ".*?/(v.*?)[-@]\\w+".r

  val supportedManufacturer: Manufacturer = Manufacturer("Shelly")

  override def parseVersion(version: String): Either[MalformedVersion, Version] = version match {
    case shellyApiVersionPattern(version) => Right(Version(version))
    case version => Left(MalformedVersion(version))
  }

  override def toDomain(hassDevice: HassResult): IO[MalformedVersion | ParseError, Device] = {
    val device: IO[MalformedVersion | String, Device] = for {
      refinedModel <- ZIO.fromEither(hassDevice.model_id.map(Model.from).getOrElse(Left("Model Id is empty")))
      refinedUrl <- ZIO.fromEither(hassDevice.configuration_url.map(UrlString.from).getOrElse(Left("Configuration URL is empty")))
      refinedId <- ZIO.fromEither(NonEmptyString.from(hassDevice.id))
      refinedName <- ZIO.fromEither(NonEmptyString.from(hassDevice.name))
      refinedManufacturer <- ZIO.fromEither(hassDevice.manufacturer.map(Manufacturer.from).getOrElse(Left("Manufacturer is empty")))
      maybeRefinedSoftwareVersion <- ZIO.fromEither(hassDevice.sw_version.filterNot(_.isBlank).map(x => parseVersion(x).map(Some(_))).getOrElse(Right(None)))
    } yield Device(
      id = refinedId,
      url = refinedUrl,
      name = refinedName,
      nameByUser = hassDevice.name_by_user,
      softwareVersion = maybeRefinedSoftwareVersion,
      model = refinedModel,
      manufacturer = refinedManufacturer,
    )
    device.mapError {
      case x: MalformedVersion => x
      case x: String => ParseError(x)
    }
  }

  override def getFirmwareDownloadDetails(
      manufacturer: Manufacturer,
      model: Model,
      version: Option[Version]
  ): IO[FirmwareDownloadError, FirmwareDescriptor] = {
    given ordering: Ordering[Version] = versionOrdering

    for {
      firmwareListUrl <- ZIO.fromEither(getFirmwareListUrl(model))
        .mapError(FirmwareDownloadLinkResolutionFailed(_, manufacturer, model))
      _ <- ZIO.logInfo(s"Getting firmware list from: $firmwareListUrl")
      firmwareList <- httpClient.getJson[Seq[ShellyFirmwareEntry]](firmwareListUrl.toString)
        .mapError {
          case e: ParseError => FailedToParseFirmwareResponse(e.message, manufacturer, model, Some(e))
          case e => FirmwareDownloadFailed(e.getMessage, manufacturer, model, Some(e))
        }
      maybeFirmwareEntry = version match {
        case Some(version) => firmwareList.find(_.version == version)
        case None => firmwareList.maxByOption(_.version)
      }
      firmware <- ZIO.fromOption(maybeFirmwareEntry)
        .mapError(_ => FirmwareNotFound("Firmware not found", manufacturer, model, version))
      firmwareDownloadUrl <- ZIO.fromEither(getFirmwareDownloadUrl(firmware))
        .mapError(FirmwareDownloadLinkResolutionFailed(_, manufacturer, model))
    } yield FirmwareDescriptor(manufacturer, model, firmwareDownloadUrl, firmware.version)
  }

  override def versionOrdering: Ordering[Version] = SemanticVersion.Ordering

  private def getFirmwareListUrl(model: Model): Either[String, UrlString] = UrlString.from(
    shellyConfig.firmwareListUrlPattern.replace("{{model}}", model.toString)
  )

  private def getFirmwareDownloadUrl(firmwareEntry: ShellyFirmwareEntry): Either[String, UrlString] = UrlString.from(
    shellyConfig.firmwareDownloadUrlPattern
      .replace("{{file}}", firmwareEntry.file)
      .replace("{{version}}", firmwareEntry.version.value)
  )

  private def callOta(device: Device): IO[DeviceApiError, Ota] = {
    val endpoint = ApiEndpoints.ota(device.url)
    for {
      _ <- ZIO.logInfo(s"Getting current firmware version from device: ${device.id} (${device.name}). Url: $endpoint")
      otaResponse <- httpClient.getJson[Ota](endpoint)
        .mapError {
          case e: ParseError => FailedToParseApiResponse(e.message, device, Some(e))
          case e => ApiCallFailed(e.getMessage, device, Some(e))
        }
    } yield otaResponse
  }

  private def resolveGetFirmwareEndpoint(firmware: Firmware): String = {
    val manufacturer = firmware.manufacturer.toString
    val model = firmware.model.toString
    val url = Path.decode(serverConfig.host) / "firmware" / manufacturer / model
    s"http://${url.toString}"
  }

  override def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, Version] = for {
    otaResponse <- callOta(device)
    version <- ZIO.fromEither(parseVersion(otaResponse.old_version))
  } yield version

  override def flashFirmware(device: Device, firmware: Firmware): IO[DeviceApiError, FlashResult] = {
    for {
      otaUrl <- ZIO
        .fromEither(URL.decode(ApiEndpoints.ota(device.url)))
        .map(_.setQueryParams("url" -> Chunk.succeed(resolveGetFirmwareEndpoint(firmware))))
        .mapError(e => ApiCallFailed("Failed to form the request URL", device, Some(e)))
      _ <- ZIO.logInfo(s"Flashing firmware to device: ${device.id} (${device.name}). Url: $otaUrl")
      response <- httpClient.getJson[Ota](otaUrl.toJavaURI.toString)
        .logError("Failed to flash firmware")
        .mapError(e => ApiCallFailed(e.getMessage, device, Some(e)))
      _ <- ZIO.logInfo(s"Flashing firmware to device: ${device.id} (${device.name}). Response: $response")
      result <- retry(checkFirmwareVersion(device, firmware.version))
    } yield result
  }

  private def checkFirmwareVersion(device: Device, expectedVersion: Version): IO[DeviceApiError, FlashResult] = for {
    currentVersion <- getCurrentFirmwareVersion(device)
    _ <- ZIO.logInfo(s"Checking if firmware updated. Device: ${device.id} (${device.name}). Current version: $currentVersion. Expected version: $expectedVersion")
  } yield FlashResult(
    previousVersion = expectedVersion,
    currentVersion = currentVersion,
    updateStatus = if (expectedVersion == currentVersion) UpdateStatus.done else UpdateStatus.updating,
  )

  private def retry(action: IO[DeviceApiError, FlashResult]): IO[DeviceApiError, FlashResult] = {
    val schedule = Schedule.recurUntil[FlashResult](_.updateStatus == UpdateStatus.done) <* Schedule.upTo(shellyConfig.firmwareFlashTimeout.seconds)
    action.repeat(schedule)
  }

  override def getDeviceStatus(device: Device): IO[DeviceApiError, DeviceStatus] = for {
    otaResponse <- callOta(device)
  } yield DeviceStatus(otaResponse.status.mapToUpdateStatus)

  def restartDevice(device: Device): IO[DeviceApiError, Unit] = {
    httpClient.get(ApiEndpoints.reboot(device.url))
      .mapError(e => ApiCallFailed(e.getMessage, device, Some(e)))
      .map(_ => ())
  }
}

object ShellyDeviceHandler {

  val layer: URLayer[HttpClient & ShellyConfig & HttpServerConfig, ShellyDeviceHandler] = ZLayer {
    for {
      httpClient <- ZIO.service[HttpClient]
      shellyConfig <- ZIO.service[ShellyConfig]
      serverConfig <- ZIO.service[HttpServerConfig]
    } yield ShellyDeviceHandler(serverConfig, shellyConfig, httpClient)
  }

  val configuredLayer: RLayer[HttpClient, ShellyDeviceHandler] = (ShellyConfig.layer ++ HttpServerConfig.layer) >>> layer

  case class ShellyFirmwareEntry(
      version: Version,
      file: String,
  )

  object ShellyFirmwareEntry {

    import is.valsk.esper.domain.Version.decoder

    given decoder: JsonDecoder[ShellyFirmwareEntry] = DeriveJsonDecoder.gen[ShellyFirmwareEntry]
  }

  object ApiEndpoints {
    def ota(baseUrl: UrlString): String = s"$baseUrl/ota"

    def reboot(baseUrl: UrlString): String = s"$baseUrl/reboot"
  }
}
