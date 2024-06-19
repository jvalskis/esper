package is.valsk.esper.config

import is.valsk.esper.config.Configs.makeLayer
import zio.{Config, Layer}

case class EmailConfig(
    from: String,
    recipients: String,
    smtp: SmtpConfig,
)

case class SmtpConfig(
    auth: Boolean,
    tls: Boolean,
    ssl: Boolean,
    host: String,
    port: Int,
    username: String,
    password: String,
)

object EmailConfig {

  val layer: Layer[Config.Error, EmailConfig] = makeLayer("esper", "mail")
}
