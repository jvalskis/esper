package is.valsk.esper.api.firmware

import is.valsk.esper.api.BaseController
import is.valsk.esper.api.firmware.endpoints.*
import is.valsk.esper.domain.Types.{ManufacturerExtractor, ModelExtractor}
import is.valsk.esper.domain.Version
import is.valsk.esper.http.endpoints.FirmwareEndpoints
import sttp.tapir.server.ServerEndpoint
import zio.{Task, URLayer, ZLayer}

class FirmwareApi(
    getFirmware: GetFirmware,
    listFirmware: ListFirmwareVersions,
    downloadFirmware: DownloadFirmware,
    downloadLatestFirmware: DownloadLatestFirmware,
    deleteFirmware: DeleteFirmware,
) extends FirmwareEndpoints with BaseController {

  val listFirmwareVersionsEndpointImpl: ServerEndpoint[Any, Task] = listFirmwareVersionsEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model)) =>
    listFirmware(manufacturer, model).either
  }
  val getFirmwareEndpointImpl: ServerEndpoint[Any, Task] = getFirmwareEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model), Version(version)) =>
    getFirmware(manufacturer, model, Some(version)).either
  }
  val getLatestFirmwareEndpointImpl: ServerEndpoint[Any, Task] = getLatestFirmwareEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model)) =>
    getFirmware(manufacturer, model, None).either
  }
  val downloadFirmwareEndpointImpl: ServerEndpoint[Any, Task] = downloadFirmwareEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model), Version(version)) =>
    downloadFirmware(manufacturer, model, version).either
  }
  val downloadLatestFirmwareEndpointImpl: ServerEndpoint[Any, Task] = downloadLatestFirmwareEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model)) =>
    downloadLatestFirmware(manufacturer, model).either
  }
  val deleteFirmwareEndpointImpl: ServerEndpoint[Any, Task] = deleteFirmwareEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model), Version(version)) =>
    deleteFirmware(manufacturer, model, version).either
  }

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    listFirmwareVersionsEndpointImpl,
    getFirmwareEndpointImpl,
    getLatestFirmwareEndpointImpl,
    downloadFirmwareEndpointImpl,
    downloadLatestFirmwareEndpointImpl,
    deleteFirmwareEndpointImpl,
  )
}

object FirmwareApi {

  val layer: URLayer[ListFirmwareVersions & GetFirmware & DownloadLatestFirmware & DownloadFirmware & DeleteFirmware, FirmwareApi] = ZLayer.fromFunction(FirmwareApi(_, _, _, _, _))
}