package is.valsk.esper.api

import is.valsk.esper.api.FirmwareApi
import is.valsk.esper.api.firmware.*
import is.valsk.esper.domain.Types.{ManufacturerExtractor, ModelExtractor}
import is.valsk.esper.domain.Version
import is.valsk.esper.services.FirmwareDownloader
import zio.http.*
import zio.http.model.{HttpError, Method, Status}
import zio.{URLayer, ZIO, ZLayer}

class FirmwareApi(
    getFirmware: GetFirmware,
    getLatestFirmware: GetLatestFirmware,
    downloadFirmware: DownloadFirmware,
    downloadLatestFirmware: DownloadLatestFirmware,
    deleteFirmware: DeleteFirmware,
) {

  val app: HttpApp[Any, HttpError] = Http.collectZIO[Request] {
    case Method.GET -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) => getLatestFirmware(manufacturer, model)
    case Method.GET -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) / Version(version) => getFirmware(manufacturer, model, version)
    case Method.POST -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) => downloadLatestFirmware(manufacturer, model)
    case Method.POST -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) / Version(version) => downloadFirmware(manufacturer, model, version)
    case Method.DELETE -> !! / "firmware" / ManufacturerExtractor(manufacturer) / ModelExtractor(model) => deleteFirmware(manufacturer, model)
    case _ => ZIO.logError("Invalid request").as(Response.status(Status.BadRequest))
  }
}

object FirmwareApi {

  val layer: URLayer[GetFirmware & DownloadLatestFirmware & DownloadFirmware & DeleteFirmware & GetLatestFirmware, FirmwareApi] = ZLayer.fromFunction(FirmwareApi(_, _, _, _, _))
}