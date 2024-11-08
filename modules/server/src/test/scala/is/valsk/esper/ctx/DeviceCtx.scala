package is.valsk.esper.ctx

import is.valsk.esper.domain.Device
import is.valsk.esper.repositories.DeviceRepository
import zio.{UIO, URIO, ZIO}

trait DeviceCtx {

  def givenDevices(devices: Device*): URIO[DeviceRepository, Unit] = for {
    deviceRepository <- ZIO.service[DeviceRepository]
    _ <- ZIO.foreach(devices)(deviceRepository.add).orDie
  } yield ()

  def givenDevicesZIO(devices: UIO[Device]*): URIO[DeviceRepository, Seq[Device]] = for {
    deviceRepository <- ZIO.service[DeviceRepository]
    addedDevices <- ZIO.foreach(devices) {
      _.flatMap(deviceRepository.add)
    }.orDie
  } yield addedDevices
}
