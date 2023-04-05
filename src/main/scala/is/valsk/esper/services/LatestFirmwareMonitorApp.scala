package is.valsk.esper.services

import is.valsk.esper.errors.EsperError
import is.valsk.esper.model.DeviceModel
import is.valsk.esper.repositories.DeviceRepository
import zio.Schedule.{WithState, exponential, identity, recurs, spaced}
import zio.{IO, Random, Ref, Schedule, Task, UIO, ZIO, ZLayer, durationInt}

class LatestFirmwareMonitorApp(
    deviceRepository: DeviceRepository,
    firmwareDownloader: FirmwareDownloader,
) {

  private def downloadTask(deviceModel: DeviceModel): IO[EsperError, DeviceModel] = for {
    _ <- ZIO.logInfo(s"Checking if there are any new versions of firmware available for $deviceModel")
    _ <- firmwareDownloader.downloadFirmware(deviceModel)
  } yield deviceModel

  def run: UIO[Unit] = for {
    _ <- ZIO.sleep(1.minute)
    modelsToMonitor <- getDistinctDeviceModels
    _ <- ZIO.foreachPar(modelsToMonitor)(scheduleFirmwareMonitor)
  } yield ()

  private def scheduleFirmwareMonitor(deviceModel: DeviceModel): UIO[Unit] = for {
    randomStartDelay <- Random.nextInt.map(_ % 5 + 1)
    _ <- ZIO.sleep(randomStartDelay.seconds)
    _ <- schedule(
      withExponentialRetry(downloadTask(deviceModel))
        .logError
        .catchAll(_ => ZIO.succeed(deviceModel))
    )
  } yield ()

  private def getDistinctDeviceModels: UIO[List[DeviceModel]] = for {
    allDevices <- deviceRepository.list
    deviceTypes = allDevices.map(device => DeviceModel(device.manufacturer, device.model)).distinct
  } yield deviceTypes

  private def withExponentialRetry(action: IO[EsperError, DeviceModel]): IO[EsperError, DeviceModel] = {
    val policy = exponential(10.seconds) && recurs(3)
    action.retry(policy)
  }

  private def schedule(action: UIO[DeviceModel]): UIO[DeviceModel] = {
    val policy = spaced(30.seconds).jittered *> identity[DeviceModel]
    action.repeat(policy)
  }
}

object LatestFirmwareMonitorApp {

  val layer: ZLayer[DeviceRepository & FirmwareDownloader, Nothing, LatestFirmwareMonitorApp] = ZLayer {
    for {
      deviceRepository <- ZIO.service[DeviceRepository]
      firmwareDownloader <- ZIO.service[FirmwareDownloader]
    } yield LatestFirmwareMonitorApp(deviceRepository, firmwareDownloader)
  }
}