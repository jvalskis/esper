package is.valsk.esper.services

import is.valsk.esper.config.EmailConfig
import is.valsk.esper.domain.EmailDeliveryError
import jakarta.mail.*
import jakarta.mail.internet.MimeMessage
import zio.{IO, Task, TaskLayer, UIO, URLayer, ZIO, ZLayer}

import java.util.Properties

trait EmailService {

  def sendEmail(subject: String, content: String): Task[Unit]
}

class EmailServiceLive private(emailConfig: EmailConfig) extends EmailService {

  val propsResource: UIO[Properties] = {
    val props = new Properties()
    props.put("mail.smtp.auth", emailConfig.smtp.auth)
    props.put("mail.smtp.starttls.enable", emailConfig.smtp.tls)
    props.put("mail.smtp.ssl.enable", emailConfig.smtp.ssl)
    props.put("mail.smtp.host", emailConfig.smtp.host)
    props.put("mail.smtp.port", emailConfig.smtp.port)
    ZIO.succeed(props)
  }

  override def sendEmail(subject: String, content: String): IO[EmailDeliveryError, Unit] = {
    for {
      props <- propsResource
      session <- createSession(props)
      message <- createMessage(session)(
        subject = subject,
        content = content
      )
      _ <- ZIO.attempt(Transport.send(message, emailConfig.smtp.username, emailConfig.smtp.password))
    } yield ()
  }
    .refineOrDie {
      case e: MessagingException => EmailDeliveryError(e.getMessage, e.getCause)
    }

  private def createSession(properties: Properties): Task[Session] = ZIO.attempt {
    Session.getInstance(properties)
  }

  private def createMessage(session: Session)(subject: String, content: String): UIO[MimeMessage] = {
    val message = new MimeMessage(session)
    message.setFrom(emailConfig.from)
    message.setRecipients(Message.RecipientType.TO, emailConfig.recipients)
    message.setSubject(subject)
    message.setContent(content, "text/html; charset=utf-8")
    ZIO.succeed(message)
  }
}

object EmailServiceLive {
  val layer: URLayer[EmailConfig, EmailService] = ZLayer {
    for {
      emailConfig <- ZIO.service[EmailConfig]
    } yield new EmailServiceLive(emailConfig)
  }
  val configuredLayer: TaskLayer[EmailService] = EmailConfig.layer >>> layer
}