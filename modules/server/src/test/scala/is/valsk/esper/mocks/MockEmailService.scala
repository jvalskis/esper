package is.valsk.esper.mocks

import is.valsk.esper.services.EmailService
import zio.mock.Mock
import zio.{Task, URLayer, ZIO, ZLayer, mock}

object MockEmailService extends Mock[EmailService] {
  object SendEmail extends Effect[(String, String), Throwable, Unit]

  val compose: URLayer[mock.Proxy, EmailService] =
    ZLayer {
      for {
        proxy <- ZIO.service[mock.Proxy]
      } yield new EmailService {
        override def sendEmail(subject: String, content: String): Task[Unit] = proxy(SendEmail, subject, content)
      }
    }
}