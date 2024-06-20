package is.valsk.esper.http.endpoints

import is.valsk.esper.domain.Version
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody

trait FirmwareEndpoints extends BaseEndpoint {

  val listFirmwareVersionsEndpoint: Endpoint[Unit, (String, String), Throwable, List[Version], Any] = baseEndpoint
    .tag("firmware")
    .name("listFirmwares")
    .description("List firmware versions for a specific manufacturer and model")
    .in("firmware" / path[String]("manufacturer") / path[String]("model") / "list")
    .get
    .out(jsonBody[List[Version]])

  val getFirmwareEndpoint: Endpoint[Unit, (String, String, String), Throwable, Array[Byte], Any] = baseEndpoint
    .tag("firmware")
    .name("getFirmware")
    .description("Get firmware by manufacturer, model and version")
    .in("firmware" / path[String]("manufacturer") / path[String]("model") / path[String]("version"))
    .get
    .out(byteArrayBody)

  val getLatestFirmwareEndpoint: Endpoint[Unit, (String, String), Throwable, Array[Byte], Any] = baseEndpoint
    .tag("firmware")
    .name("getFirmware")
    .description("Get latest firmware by manufacturer and model")
    .in("firmware" / path[String]("manufacturer") / path[String]("model"))
    .get
    .out(byteArrayBody)

  val downloadFirmwareEndpoint: Endpoint[Unit, (String, String, String), Throwable, Unit, Any] = baseEndpoint
    .tag("firmwares")
    .name("downloadFirmware")
    .description("Download firmware for a specific device manufacturer, model and version")
    .in("firmwares" / path[String]("manufacturer") / path[String]("model") / path[String]("version"))
    .post

  val downloadLatestFirmwareEndpoint: Endpoint[Unit, (String, String), Throwable, Unit, Any] = baseEndpoint
    .tag("firmwares")
    .name("downloadLatestFirmware")
    .description("Download latest firmware for a specific device manufactuer and model")
    .in("firmwares" / path[String]("manufacturer") / path[String]("model"))
    .post

  val deleteFirmwareEndpoint: Endpoint[Unit, (String, String, String), Throwable, Unit, Any] = baseEndpoint
    .tag("firmwares")
    .name("deleteFirmware")
    .description("Delete firmware for a specific device manufacturer and model")
    .in("firmwares" / path[String]("manufacturer") / path[String]("model") / path[String]("version"))
    .delete
}
