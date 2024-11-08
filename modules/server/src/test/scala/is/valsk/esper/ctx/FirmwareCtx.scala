package is.valsk.esper.ctx

import is.valsk.esper.domain.Firmware
import is.valsk.esper.repositories.FirmwareRepository
import zio.{URIO, ZIO}

trait FirmwareCtx {

  def givenFirmwares(firmwares: Firmware*): URIO[FirmwareRepository, Unit] = for {
    firmwareRepository <- ZIO.service[FirmwareRepository]
    _ <- ZIO.foreach(firmwares)(firmwareRepository.add).orDie
  } yield ()
}
