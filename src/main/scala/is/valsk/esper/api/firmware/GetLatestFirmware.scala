package is.valsk.esper.api.firmware

import is.valsk.esper.api.FirmwareApi
import is.valsk.esper.device.DeviceManufacturerHandler
import is.valsk.esper.domain.Types.{Manufacturer, ManufacturerExtractor, Model, ModelExtractor}
import is.valsk.esper.domain.{DeviceModel, PersistenceException, Version}
import is.valsk.esper.repositories.FirmwareRepository.FirmwareKey
import is.valsk.esper.repositories.{FirmwareRepository, ManufacturerRepository}
import is.valsk.esper.services.FirmwareDownloader
import zio.http.*
import zio.http.model.HttpError.NotFound
import zio.http.model.{Headers, HttpError, Status}
import zio.stream.ZStream
import zio.{IO, URLayer, ZIO, ZLayer}

class GetLatestFirmware(
    firmwareRepository: FirmwareRepository,
    manufacturerRepository: ManufacturerRepository,
) {

  def apply(manufacturer: Manufacturer, model: Model): IO[HttpError, Response] = for {
    manufacturerHandler <- manufacturerRepository.get(manufacturer)
      .mapError(_ => HttpError.InternalServerError())
      .flatMap {
        case Some(handler) => ZIO.succeed(handler)
        case None => ZIO.fail(NotFound(""))
      }
    latestFirmware <- firmwareRepository.getAll
      .mapError(_ => HttpError.InternalServerError())
      .flatMap {
        case Nil => ZIO.fail(NotFound(""))
        case list => ZIO.succeed(list)
      }
      .map(_
        .filter(fw => fw.deviceModel.manufacturer == manufacturer && fw.deviceModel.model == model)
        .maxBy(_.version)(manufacturerHandler.versionOrdering)
      )
  } yield Response(
    status = Status.Ok,
    headers = Headers.contentLength(latestFirmware.data.size),
    body = Body.fromChunk(latestFirmware.data)
  )
}

object GetLatestFirmware {

  val layer: URLayer[FirmwareRepository & ManufacturerRepository, GetLatestFirmware] = ZLayer {
    for {
      firmwareRepository <- ZIO.service[FirmwareRepository]
      manufacturerRepository <- ZIO.service[ManufacturerRepository]
    } yield GetLatestFirmware(firmwareRepository, manufacturerRepository)
  }
}