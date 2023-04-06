package is.valsk.esper.api

import is.valsk.esper.api.FirmwareApi
import is.valsk.esper.api.firmware.{DeleteFirmware, DownloadFirmware, GetFirmware}
import is.valsk.esper.domain.Types.{ManufacturerExtractor, ModelExtractor}
import is.valsk.esper.services.FirmwareDownloader
import zio.http.model.Method
import zio.http.*
import zio.{URLayer, ZIO, ZLayer}

class FirmwareApi(
    getFirmware: GetFirmware,
    downloadFirmware: DownloadFirmware,
    deleteFirmware: DeleteFirmware,
) {

  val app: HttpApp[Any, Response] = Http.collectZIO[Request] {
    case Method.GET -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) => getFirmware(manufacturer, model)
    case Method.POST -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) => downloadFirmware(manufacturer, model)
    case Method.DELETE -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) => deleteFirmware(manufacturer, model)
  }
}

object FirmwareApi {

  val layer: URLayer[GetFirmware & DownloadFirmware & DeleteFirmware, FirmwareApi] = ZLayer {
    for {
      getFirmware <- ZIO.service[GetFirmware]
      downloadFirmware <- ZIO.service[DownloadFirmware]
      deleteFirmware <- ZIO.service[DeleteFirmware]
    } yield FirmwareApi(getFirmware, downloadFirmware, deleteFirmware)
  }
}