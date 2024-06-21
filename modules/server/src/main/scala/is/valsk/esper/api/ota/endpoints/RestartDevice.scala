package is.valsk.esper.api.ota.endpoints

import is.valsk.esper.domain.EsperError
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.services.OtaService
import zio.{IO, URLayer, ZLayer}

class RestartDevice(
    otaService: OtaService
) {

  def apply(deviceId: DeviceId): IO[EsperError, Unit] =
    otaService.restartDevice(deviceId)
}

object RestartDevice {

  val layer: URLayer[OtaService, RestartDevice] = ZLayer.fromFunction(RestartDevice(_))

}
