package is.valsk.esper.device.shelly

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.device.shelly.ShellyDevice.{ApiEndpoints, ShellyFirmwareEntry}
import is.valsk.esper.device.shelly.api.Ota
import is.valsk.esper.device.shelly.api.Ota.decoder
import is.valsk.esper.device.{DeviceManufacturerHandler, DeviceProxy}
import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.{Model, UrlString}
import is.valsk.esper.hass.HassToDomainMapper
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.services.HttpClient
import zio.http.{Client, ClientConfig}
import zio.json.*
import zio.{IO, ULayer, URLayer, ZIO, ZLayer}

import scala.annotation.tailrec
import scala.util.Try

class ShellyDevice(
    shellyConfig: ShellyConfig,
    httpClient: HttpClient,
) extends DeviceManufacturerHandler with HassToDomainMapper with DeviceProxy[SemanticVersion] {

  private val hardwareAndModelRegex = "(.+) \\((.+)\\)".r
  private val shellyApiVersionPattern = ".*?/(v.*?)[-@]\\w+".r

  override def getCurrentFirmwareVersion(device: Device): IO[DeviceApiError, SemanticVersion] = {
    val endpoint = ApiEndpoints.ota(device.url)
    for {
      _ <- ZIO.logInfo(s"Getting current firmware version from device: ${device.id} (${device.name}). Url: $endpoint")
      otaResponse <- httpClient.getJson[Ota](endpoint)
        .mapError {
          case e: ParseError => FailedToParseApiResponse(e.message, device, Some(e))
          case e => ApiCallFailed(e.getMessage, device, Some(e))
        }
      version <- otaResponse.old_version match {
        case shellyApiVersionPattern(version) => ZIO.succeed(SemanticVersion(version))
        case version => ZIO.fail(MalformedVersion(version, device: Device))
      }
    } yield version
  }

  override def toDomain(hassDevice: HassResult): IO[String, Device] = ZIO.fromEither(
    for {
      refinedModel <- resolveModel(hassDevice)
      refinedUrl <- hassDevice.configuration_url.map(UrlString.from).getOrElse(Left("Configuration URL is empty"))
      refinedId <- NonEmptyString.from(hassDevice.id)
      refinedName <- NonEmptyString.from(hassDevice.name)
      refinedManufacturer <- NonEmptyString.from(hassDevice.manufacturer)
    } yield Device(
      id = refinedId,
      url = refinedUrl,
      name = refinedName,
      nameByUser = hassDevice.name_by_user,
      softwareVersion = hassDevice.sw_version,
      model = refinedModel,
      manufacturer = refinedManufacturer,
    )
  )

  override def getFirmwareDownloadDetails(deviceModel: DeviceModel): IO[FirmwareDownloadError, FirmwareDescriptor] = {
    for {
      firmwareListUrl <- ZIO.fromEither(getFirmwareListUrl(deviceModel.model))
        .mapError(FirmwareDownloadLinkResolutionFailed(_, deviceModel))
      _ <- ZIO.logInfo(s"Getting firmware list from: $firmwareListUrl")
      firmwareList <- httpClient.getJson[Seq[ShellyFirmwareEntry]](firmwareListUrl.toString)
        .mapError {
          case e: ParseError => FailedToParseFirmwareResponse(e.message, deviceModel, Some(e))
          case e => FirmwareDownloadFailed(deviceModel, Some(e))
        }
      latestFirmware = firmwareList.max
      latestFirmwareDownloadUrl <- ZIO.fromEither(getFirmwareDownloadUrl(latestFirmware))
        .mapError(FirmwareDownloadLinkResolutionFailed(_, deviceModel))
    } yield FirmwareDescriptor(deviceModel, latestFirmwareDownloadUrl, latestFirmware.version)
  }

  private def getFirmwareListUrl(model: Model): Either[String, UrlString] = UrlString.from(
    shellyConfig.firmwareListUrlPattern.replace("{{model}}", model.toString)
  )

  private def getFirmwareDownloadUrl(firmwareEntry: ShellyFirmwareEntry): Either[String, UrlString] = UrlString.from(
    shellyConfig.firmwareDownloadUrlPattern
      .replace("{{file}}", firmwareEntry.file)
      .replace("{{version}}", firmwareEntry.version.value)
  )

  private def resolveModel(hassDevice: HassResult): Either[String, Model] = {
    hassDevice.hw_version
      .flatMap {
        case hardwareAndModelRegex(_, model) => Some(model)
        case _ => None
      }
      .map(Right(_))
      .getOrElse(Left("Invalid hw_version format"))
      .flatMap(Model.from)
  }
}

object ShellyDevice {

  val layer: URLayer[HttpClient & ShellyConfig, ShellyDevice] = ZLayer {
    for {
      httpClient <- ZIO.service[HttpClient]
      shellyConfig <- ZIO.service[ShellyConfig]
    } yield ShellyDevice(shellyConfig, httpClient)
  }

  case class ShellyFirmwareEntry(
      version: SemanticVersion,
      file: String,
  ) extends Ordered[ShellyFirmwareEntry] {

    override def compare(that: ShellyFirmwareEntry): Int = version.compare(that.version)
  }

  object ShellyFirmwareEntry {

    import SemanticVersion.decoder

    implicit val decoder: JsonDecoder[ShellyFirmwareEntry] = DeriveJsonDecoder.gen[ShellyFirmwareEntry]
  }

  object ApiEndpoints {
    def ota(baseUrl: UrlString): String = s"$baseUrl/ota"
  }
}
