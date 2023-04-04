package is.valsk.esper.device.shelly

import eu.timepit.refined.types.string.NonEmptyString
import is.valsk.esper.device.DeviceDescriptor
import is.valsk.esper.device.shelly.ShellyDevice.ShellyFirmwareEntry
import is.valsk.esper.errors.{EsperError, FirmwareDownloadError, FirmwareDownloadFailed, FirmwareDownloadLinkResolutionFailed, FailedToParseFirmwareResponse}
import is.valsk.esper.hass.device.DeviceManufacturerHandler
import is.valsk.esper.hass.device.DeviceManufacturerHandler.FirmwareDescriptor
import is.valsk.esper.hass.messages.MessageParser.ParseError
import is.valsk.esper.hass.messages.responses.HassResult
import is.valsk.esper.http.HttpClient
import is.valsk.esper.model.Device
import is.valsk.esper.types.{Model, UrlString}
import is.valsk.esper.utils.SemanticVersion
import zio.http.{Client, ClientConfig}
import zio.json.*
import zio.{IO, ULayer, URLayer, ZIO, ZLayer}

import scala.annotation.tailrec
import scala.util.Try

class ShellyDevice(
    shellyConfig: ShellyConfig,
    httpClient: HttpClient,
) extends DeviceManufacturerHandler {

  private val hardwareAndModelRegex = "(.+) \\((.+)\\)".r

  private def getFirmwareListUrl(model: Model): Either[String, UrlString] = UrlString.from(
    shellyConfig.firmwareListUrlPattern.replace("{{model}}", model.toString)
  )

  private def getFirmwareDownloadUrl(firmwareEntry: ShellyFirmwareEntry): Either[String, UrlString] = UrlString.from(
    shellyConfig.firmwareDownloadUrlPattern
      .replace("{{file}}", firmwareEntry.file)
      .replace("{{version}}", firmwareEntry.version.value)
  )

  override def toDomain(hassDevice: HassResult): IO[String, Device] = ZIO.fromEither(
    for {
      hardwareModel <- resolveHardwareModel(hassDevice)
      (hardware, model) = hardwareModel
      refinedUrl <- hassDevice.configuration_url.map(UrlString.from).getOrElse(Left("Configuration URL is empty"))
      refinedId <- NonEmptyString.from(hassDevice.id)
      refinedName <- NonEmptyString.from(hassDevice.name)
      refinedModel <- NonEmptyString.from(model)
      refinedManufacturer <- NonEmptyString.from(hassDevice.manufacturer)
    } yield Device(
      id = refinedId,
      url = refinedUrl,
      name = refinedName,
      nameByUser = hassDevice.name_by_user,
      softwareVersion = hassDevice.sw_version,
      hardware = Some(hardware),
      model = refinedModel,
      manufacturer = refinedManufacturer,
    )
  )

  override def getFirmwareDownloadDetails(deviceDescriptor: DeviceDescriptor): IO[FirmwareDownloadError, FirmwareDescriptor] = {
    for {
      firmwareListUrl <- ZIO.fromEither(getFirmwareListUrl(deviceDescriptor.model))
        .mapError(FirmwareDownloadLinkResolutionFailed(_, deviceDescriptor))
      _ <- ZIO.logInfo(s"Getting firmware list from: $firmwareListUrl")
      firmwareList <- httpClient.get(firmwareListUrl.toString)
        .flatMap(_.body.asString)
        .flatMap(response => ZIO
          .fromEither(response.fromJson[Seq[ShellyFirmwareEntry]])
          .mapError(FailedToParseFirmwareResponse(_, deviceDescriptor))
        )
        .mapError(e => FirmwareDownloadFailed(deviceDescriptor, Some(e)))
      latestFirmware = firmwareList.max
      latestFirmwareDownloadUrl <- ZIO.fromEither(getFirmwareDownloadUrl(latestFirmware))
        .mapError(FirmwareDownloadLinkResolutionFailed(_, deviceDescriptor))
    } yield FirmwareDescriptor(deviceDescriptor, latestFirmwareDownloadUrl, latestFirmware.version)
  }

  private def resolveHardwareModel(hassDevice: HassResult) = {
    hassDevice.hw_version
      .flatMap {
        case hardwareAndModelRegex(hardware, model) => Some((hardware, model))
        case _ => None
      }
      .map(Right(_))
      .getOrElse(Left("Invalid hw_version format"))
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

    import is.valsk.esper.utils.SemanticVersion.decoder

    implicit val decoder: JsonDecoder[ShellyFirmwareEntry] = DeriveJsonDecoder.gen[ShellyFirmwareEntry]
  }
}
