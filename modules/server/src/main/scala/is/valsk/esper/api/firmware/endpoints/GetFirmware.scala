package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.domain.{EsperError, Version}
import is.valsk.esper.services.FirmwareService
import zio.{IO, URLayer, ZLayer}

class GetFirmware(
    firmwareService: FirmwareService,
) {

  def apply(manufacturer: Manufacturer, model: Model, maybeVersion: Option[Version] = None): IO[EsperError, Array[Byte]] = for {
    firmware <- maybeVersion match
      case Some(version) =>
        firmwareService.getFirmware(manufacturer, model, version)
      case None =>
        firmwareService.getLatestFirmware(manufacturer, model)
  } yield firmware.data 
//    Response(
//    status = Status.Ok,
//    headers = Headers.contentLength(firmware.size) ++ Headers.contentType("application/zip"),
//    body = Body.fromChunk(Chunk.from(firmware.data)),
//  )
}

object GetFirmware {

  val layer: URLayer[FirmwareService, GetFirmware] = ZLayer.fromFunction(GetFirmware(_))
}