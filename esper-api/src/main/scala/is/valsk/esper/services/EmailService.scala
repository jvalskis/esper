package is.valsk.esper.services

import is.valsk.esper.config.EmailConfig
import jakarta.mail.internet.{InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart}
import jakarta.mail.{Authenticator, Message, PasswordAuthentication, Session, Transport}
import zio.{Task, TaskLayer, UIO, URLayer, ZIO, ZLayer}

import java.util.Properties

trait EmailService {

  def sendEmail(subject: String, content: String): Task[Unit]
}

class EmailServiceLive private(emailConfig: EmailConfig) extends EmailService {

  val propsResource: UIO[Properties] = {
    val props = new Properties()
    props.put("mail.smtp.auth", emailConfig.smtp.auth)
    props.put("mail.smtp.starttls.enable", "true")
    props.put("mail.smtp.host", emailConfig.smtp.host)
    props.put("mail.smtp.port", emailConfig.smtp.port)
    ZIO.succeed(props)
  }

  override def sendEmail(subject: String, content: String): Task[Unit] = for {
    props <- propsResource
    session <- createSession(props)
    message <- createMessage(session)(
      subject = subject,
      content = content
    )
    _ <- ZIO.attempt(Transport.send(message))
  } yield ()

  private def createSession(properties: Properties): Task[Session] = ZIO.attempt {
    Session.getInstance(properties, new Authenticator {
      override protected def getPasswordAuthentication = new PasswordAuthentication(emailConfig.smtp.username, emailConfig.smtp.password)
    })
  }

  private def createMessage(session: Session)(subject: String, content: String): Task[MimeMessage] = {
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