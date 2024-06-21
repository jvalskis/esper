package is.valsk.esper.api.ota.endpoints

import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.domain.{EsperError, Version}
import is.valsk.esper.services.OtaService
import zio.{IO, URLayer, ZLayer}

class GetDeviceVersion(
    otaService: OtaService
) {

  def apply(deviceId: DeviceId): IO[EsperError, Version] =
    otaService.getCurrentFirmwareVersion(deviceId)
}

object GetDeviceVersion {

  val layer: URLayer[OtaService, GetDeviceVersion] = ZLayer.fromFunction(GetDeviceVersion(_))

}
