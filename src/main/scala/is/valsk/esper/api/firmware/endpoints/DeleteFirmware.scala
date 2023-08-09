package is.valsk.esper.api.firmware.endpoints

import is.valsk.esper.domain.Types.{Manufacturer, Model}
import is.valsk.esper.repositories.FirmwareRepository
import zio.http.Response
import zio.http.model.HttpError
import zio.{IO, URLayer, ZLayer}

class DeleteFirmware(
    firmwareRepository: FirmwareRepository,
) {

  def apply(manufacturer: Manufacturer, model: Model): IO[HttpError, Response] = ???
}

object DeleteFirmware {

  val layer: URLayer[FirmwareRepository, DeleteFirmware] = ZLayer.fromFunction(DeleteFirmware(_))
}