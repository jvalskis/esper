package is.valsk.esper.api.devices

import is.valsk.esper.api.ApiSpec
import is.valsk.esper.domain.Types.DeviceId
import sttp.client3.testing.SttpBackendStub
import sttp.client3.{Response, UriContext, basicRequest}
import sttp.monad.MonadError
import sttp.tapir.server.stub.TapirStubInterpreter
import sttp.tapir.ztapir.RIOMonadError
import zio.{Task, ZIO}

trait DevicesSpec extends ApiSpec {

  private given zioMonadError: MonadError[Task] = new RIOMonadError[Any]

  def invokeGetDevice(deviceId: DeviceId): ZIO[DeviceApi, Throwable, Response[Either[String, String]]] = for {
    deviceApi <- ZIO.service[DeviceApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(deviceApi.getDeviceEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .get(uri"/api/devices/$deviceId")
      .send(backendStub)
  } yield response

  def invokeGetPendingUpdate(deviceId: DeviceId): ZIO[DeviceApi, Throwable, Response[Either[String, String]]] = for {
    deviceApi <- ZIO.service[DeviceApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(deviceApi.getPendingUpdateEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .get(uri"/api/devices/updates/$deviceId")
      .send(backendStub)
  } yield response

  def invokeGetPendingUpdates: ZIO[DeviceApi, Throwable, Response[Either[String, String]]] = for {
    deviceApi <- ZIO.service[DeviceApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(deviceApi.getPendingUpdatesEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .get(uri"/api/devices/updates/list")
      .send(backendStub)
  } yield response

  def invokeListDevices: ZIO[DeviceApi, Throwable, Response[Either[String, String]]] = for {
    deviceApi <- ZIO.service[DeviceApi]
    backendStub <- ZIO.succeed(
      TapirStubInterpreter(SttpBackendStub(MonadError[Task]))
        .whenServerEndpointRunLogic(deviceApi.listDevicesEndpointImpl)
        .backend()
    )
    response <- basicRequest
      .get(uri"/api/devices")
      .send(backendStub)
  } yield response
}
