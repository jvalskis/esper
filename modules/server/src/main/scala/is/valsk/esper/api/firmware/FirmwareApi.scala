package is.valsk.esper.api.firmware

import is.valsk.esper.api.BaseController
import is.valsk.esper.api.firmware.endpoints.*
import is.valsk.esper.domain.Types.{ManufacturerExtractor, ModelExtractor}
import is.valsk.esper.domain.Version
import is.valsk.esper.http.endpoints.FirmwareEndpoints
import sttp.tapir.server.ServerEndpoint
import zio.{Task, URLayer, ZIO, ZLayer}

class FirmwareApi(
    getFirmware: GetFirmware,
    listFirmware: ListFirmwareVersions,
    downloadFirmware: DownloadFirmware,
    downloadLatestFirmware: DownloadLatestFirmware,
    deleteFirmware: DeleteFirmware,
) extends FirmwareEndpoints with BaseController {

  override val routes: List[ServerEndpoint[Any, Task]] = List(
    listFirmwareVersionsEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model)) =>
      listFirmware(manufacturer, model).either
    },
    getFirmwareEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model), Version(version)) =>
      getFirmware(manufacturer, model, Some(version)).either
    },
    getLatestFirmwareEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model)) =>
      getFirmware(manufacturer, model, None).either
    },
    downloadFirmwareEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model), Version(version)) =>
      downloadFirmware(manufacturer, model, version).either
    },
    downloadLatestFirmwareEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model)) =>
      downloadLatestFirmware(manufacturer, model).either
    },
    deleteFirmwareEndpoint.serverLogic { case (ManufacturerExtractor(manufacturer), ModelExtractor(model), Version(version)) =>
      deleteFirmware(manufacturer, model, version).either
    },
  )
}

object FirmwareApi {

  val layer: URLayer[ListFirmwareVersions & GetFirmware & DownloadLatestFirmware & DownloadFirmware & DeleteFirmware, FirmwareApi] = ZLayer.fromFunction(FirmwareApi(_, _, _, _, _))
}