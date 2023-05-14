package is.valsk.esper.api.firmware

import is.valsk.esper.api.FirmwareApi
import is.valsk.esper.domain.DeviceModel
import is.valsk.esper.domain.Types.{Manufacturer, ManufacturerExtractor, Model, ModelExtractor}
import is.valsk.esper.repositories.FirmwareRepository
import is.valsk.esper.services.FirmwareDownloader
import zio.http.model.HttpError
import zio.http.{Http, HttpApp, Request, Response}
import zio.{IO, URLayer, ZIO, ZLayer}

class DeleteFirmware(
    firmwareRepository: FirmwareRepository,
) {

  def apply(manufacturer: Manufacturer, model: Model): IO[HttpError, Response] = ???
}

object DeleteFirmware {

  val layer: URLayer[FirmwareRepository, DeleteFirmware] = ZLayer.fromFunction(DeleteFirmware(_))
}