package is.valsk.esper.services

import is.valsk.esper.config.FlywayConfig
import org.flywaydb.core.Flyway
import zio.{RLayer, Task, TaskLayer, ZIO, ZLayer}

trait FlywayService {
  def runClean(): Task[Unit]

  def runBaseline(): Task[Unit]

  def runMigrations(): Task[Unit]

  def runRepairs(): Task[Unit]
}

class FlywayServiceLive private(flyway: Flyway) extends FlywayService {
  override def runClean(): Task[Unit] =
    ZIO.attemptBlocking(flyway.clean()).unit

  override def runBaseline(): Task[Unit] =
    ZIO.attemptBlocking(flyway.baseline()).unit

  override def runMigrations(): Task[Unit] =
    ZIO.attemptBlocking(flyway.migrate()).unit

  override def runRepairs(): Task[Unit] =
    ZIO.attemptBlocking(flyway.repair()).unit
}

object FlywayServiceLive {
  val layer: RLayer[FlywayConfig, FlywayService] = ZLayer {
    for {
      config <- ZIO.service[FlywayConfig]
      flyway <- ZIO.attempt(
        Flyway
          .configure()
          .dataSource(config.url, config.user, config.password)
          .load()
      )
    } yield new FlywayServiceLive(flyway)
  }

  val configuredLayer: TaskLayer[FlywayService] = FlywayConfig.layer >>> FlywayServiceLive.layer
}