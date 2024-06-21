package is.valsk.esper.api.ota.endpoints

import is.valsk.esper.domain.*
import is.valsk.esper.domain.Types.DeviceId
import is.valsk.esper.repositories.DeviceRepository
import is.valsk.esper.services.{FirmwareService, OtaService}
import zio.{IO, URLayer, ZLayer}

class FlashDevice(
    otaService: OtaService,
    deviceRepository: DeviceRepository,
    firmwareService: FirmwareService,
) {

  def apply(deviceId: DeviceId, maybeVersion: Option[Version]): IO[EsperError, FlashResult] = for {
    device <- deviceRepository.get(deviceId)
    firmware <- maybeVersion match
      case Some(version) =>
        firmwareService.getFirmware(device.manufacturer, device.model, version)
      case None =>
        firmwareService.getLatestFirmware(device.manufacturer, device.model)
    result <- otaService.flashFirmware(device, firmware)
  } yield result
}

object FlashDevice {

  val layer: URLayer[DeviceRepository & OtaService & FirmwareService, FlashDevice] = ZLayer.fromFunction(FlashDevice(_, _, _))

}
