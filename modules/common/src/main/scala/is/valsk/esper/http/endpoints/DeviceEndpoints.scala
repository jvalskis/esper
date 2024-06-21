package is.valsk.esper.http.endpoints

import is.valsk.esper.domain.Device.*
import is.valsk.esper.domain.{Device, PendingUpdate}
import sttp.tapir.*
//import sttp.tapir.codec.refined.*
import sttp.tapir.generic.auto.*
import sttp.tapir.json.zio.jsonBody

trait DeviceEndpoints extends BaseEndpoint {
  val listDevicesEndpoint: Endpoint[Unit, Unit, Throwable, List[Device], Any] = baseEndpoint
    .tag("devices")
    .name("listDevices")
    .description("List all devices")
    .in("devices")
    .get
    .out(jsonBody[List[Device]])

  val getDeviceEndpoint: Endpoint[Unit, String, Throwable, Device, Any] = baseEndpoint
    .tag("devices")
    .name("getDevice")
    .description("Get a device by id")
    .in("devices" / path[String]("deviceId"))
    .get
    .out(jsonBody[Device])

  val getPendingUpdatesEndpoint: Endpoint[Unit, Unit, Throwable, List[PendingUpdate], Any] = baseEndpoint
    .tag("devices")
    .name("getPendingUpdates")
    .description("List all devices with pending updates")
    .in("devices" / "updates")
    .get
    .out(jsonBody[List[PendingUpdate]])

  val getPendingUpdateEndpoint: Endpoint[Unit, String, Throwable, PendingUpdate, Any] = baseEndpoint
    .tag("devices")
    .name("getPendingUpdate")
    .description("Get pending update for a device by id")
    .in("devices" / "updates" / path[String]("deviceId"))
    .get
    .out(jsonBody[PendingUpdate])
}
