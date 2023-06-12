package is.valsk.esper.api.firmware

import is.valsk.esper.api.firmware.endpoints.*
import is.valsk.esper.domain.Types.{ManufacturerExtractor, ModelExtractor}
import is.valsk.esper.domain.Version
import is.valsk.esper.services.FirmwareDownloader
import zio.http.*
import zio.http.model.{HttpError, Method, Status}
import zio.{URLayer, ZIO, ZLayer}

class FirmwareApi(
    getFirmware: GetFirmware,
    listFirmware: ListFirmwareVersions,
    downloadFirmware: DownloadFirmware,
    downloadLatestFirmware: DownloadLatestFirmware,
    deleteFirmware: DeleteFirmware,
) {

  val app: HttpApp[Any, HttpError] = Http.collectZIO[Request] {
    case Method.GET -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) / "list" => listFirmware(manufacturer, model)
    case Method.GET -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) => getFirmware(manufacturer, model)
    case Method.GET -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) / Version(version) => getFirmware(manufacturer, model, Some(version))
    case Method.POST -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) => downloadLatestFirmware(manufacturer, model)
    case Method.POST -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) / Version(version) => downloadFirmware(manufacturer, model, version)
    case Method.DELETE -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) => deleteFirmware(manufacturer, model)
  }
}

object FirmwareApi {

  val layer: URLayer[ListFirmwareVersions & GetFirmware & DownloadLatestFirmware & DownloadFirmware & DeleteFirmware, FirmwareApi] = ZLayer.fromFunction(FirmwareApi(_, _, _, _, _))
}