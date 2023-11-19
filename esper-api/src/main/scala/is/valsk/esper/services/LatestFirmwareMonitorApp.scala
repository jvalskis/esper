package is.valsk.esper.services

import is.valsk.esper.ScheduleConfig
import is.valsk.esper.domain.{DeviceModel, EsperError, PersistenceException}
import is.valsk.esper.repositories.DeviceRepository
import zio.Schedule.*
import zio.{IO, RLayer, Random, Schedule, UIO, ZIO, ZLayer, durationInt}

trait LatestFirmwareMonitorApp {
  def run: UIO[Unit]
}

object LatestFirmwareMonitorApp {

  private class LatestFirmwareMonitorAppLive(
      scheduleConfig: ScheduleConfig,
      deviceRepository: DeviceRepository,
      firmwareDownloader: FirmwareDownloader,
  ) extends LatestFirmwareMonitorApp {

    private def downloadTask(deviceModel: DeviceModel): IO[EsperError, DeviceModel] = for {
      _ <- ZIO.logInfo(s"Checking if there are any new versions of firmware available for $deviceModel")
      _ <- firmwareDownloader.downloadFirmware(deviceModel.manufacturer, deviceModel.model)
    } yield deviceModel

    def run: UIO[Unit] = for {
      _ <- ZIO.sleep(scheduleConfig.initialDelay.seconds)
      modelsToMonitor <- getDistinctDeviceModels.orDie
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

    private def getDistinctDeviceModels: IO[PersistenceException, List[DeviceModel]] = for {
      allDevices <- deviceRepository.getAll
      deviceTypes = allDevices.map(device => DeviceModel(device.manufacturer, device.model)).distinct
    } yield deviceTypes

    private def withExponentialRetry(action: IO[EsperError, DeviceModel]): IO[EsperError, DeviceModel] = {
      val schedule = exponential(scheduleConfig.exponentialRetryBase.seconds) && recurs(scheduleConfig.maxRetries)
      action.retry(schedule)
    }

    private def schedule(action: UIO[DeviceModel]): UIO[DeviceModel] = {
      val baseSchedule = spaced(scheduleConfig.interval.seconds)
      val finalSchedule = if (scheduleConfig.jitter) baseSchedule.jittered else baseSchedule
      action.repeat(finalSchedule *> identity[DeviceModel])
    }
  }

  val layer: RLayer[ScheduleConfig & DeviceRepository & FirmwareDownloader, LatestFirmwareMonitorApp] = ZLayer.fromFunction(LatestFirmwareMonitorAppLive(_, _, _))
}