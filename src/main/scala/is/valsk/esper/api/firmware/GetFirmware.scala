package is.valsk.esper.api.firmware

import is.valsk.esper.api.FirmwareApi
import is.valsk.esper.domain.DeviceModel
import is.valsk.esper.domain.Types.{Manufacturer, ManufacturerExtractor, Model, ModelExtractor}
import is.valsk.esper.repositories.FirmwareRepository
import is.valsk.esper.services.FirmwareDownloader
import zio.http.model.Status
import zio.http.{Http, HttpApp, Request, Response}
import zio.{IO, URLayer, ZIO, ZLayer}

class GetFirmware(
    firmwareRepository: FirmwareRepository,
) {

  def apply(manufacturer: Manufacturer, model: Model): IO[Response, Response] = (for {
      firmware <- firmwareRepository.get(DeviceModel(manufacturer, model))
    } yield Response.status(Status.Ok))
      .mapError(_ => Response.status(Status.BadRequest))
}

object GetFirmware {

  val layer: URLayer[FirmwareRepository, GetFirmware] = ZLayer {
    for {
      firmwareRepository <- ZIO.service[FirmwareRepository]
    } yield GetFirmware(firmwareRepository)
  }
}