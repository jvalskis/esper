package is.valsk.esper.http.endpoints

import is.valsk.esper.domain.{DeviceStatus, FlashResult, Version}
import sttp.tapir.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody

trait OtaEndpoints extends BaseEndpoint {

  val getDeviceVersionEndpoint: Endpoint[Unit, String, Throwable, Version, Any] = baseEndpoint
    .tag("ota")
    .name("getDeviceVersion")
    .description("Get device version")
    .in("ota" / path[String]("deviceId") / "version")
    .get
    .out(jsonBody[Version])

  val flashDeviceEndpoint: Endpoint[Unit, (String, String), Throwable, FlashResult, Any] = baseEndpoint
    .tag("ota")
    .name("flashDevice")
    .description("Flash device with the latest firmware version")
    .in("ota" / path[String]("deviceId") / "flash" / path[String]("version"))
    .post
    .out(jsonBody[FlashResult])

  val flashDeviceWithLatestVersionEndpoint: Endpoint[Unit, String, Throwable, FlashResult, Any] = baseEndpoint
    .tag("ota")
    .name("flashDevice")
    .description("Flash device with a specific firmware version")
    .in("ota" / path[String]("deviceId") / "flash")
    .post
    .out(jsonBody[FlashResult])

  val getDeviceStatusEndpoint: Endpoint[Unit, String, Throwable, DeviceStatus, Any] = baseEndpoint
    .tag("ota")
    .name("getDeviceStatus")
    .description("Get device status")
    .in("ota" / path[String]("deviceId") / "status")
    .post
    .out(jsonBody[DeviceStatus])

  val restartDeviceEndpoint: Endpoint[Unit, String, Throwable, Unit, Any] = baseEndpoint
    .tag("ota")
    .name("restartDevice")
    .description("Restart device")
    .in("ota" / path[String]("deviceId") / "restart")
    .post
}
