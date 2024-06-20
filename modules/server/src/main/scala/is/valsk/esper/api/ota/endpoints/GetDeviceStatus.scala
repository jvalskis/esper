package is.valsk.esper.api.ota.endpoints

import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.domain.{DeviceStatus, EsperError}
import is.valsk.esper.services.OtaService
import zio.{IO, URLayer, ZLayer}

class GetDeviceStatus(
    otaService: OtaService
) {

  def apply(deviceId: DeviceId): IO[EsperError, DeviceStatus] =
    otaService.getDeviceStatus(deviceId)
}

object GetDeviceStatus {

  val layer: URLayer[OtaService, GetDeviceStatus] = ZLayer.fromFunction(GetDeviceStatus(_))

}
