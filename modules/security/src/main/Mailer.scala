package lila.security

import akka.actor.ActorSystem
import io.methvin.play.autoconfig._
import play.api.i18n.Lang
import play.api.libs.mailer.{ Email, SMTPConfiguration, SMTPMailer }
import scala.concurrent.duration.{ span => _, _ }
import scala.concurrent.{ blocking, Future }
import scalatags.Text.all._

import lila.common.config.Secret
import lila.common.String.html.{ escapeHtml, nl2brUnsafe }
import lila.common.{ Chronometer, EmailAddress, ThreadLocalRandom }
import lila.i18n.I18nKeys.{ emails => trans }

final class Mailer(
    config: Mailer.Config,
    getSecondaryPermille: () => Int
)(implicit system: ActorSystem) {

  implicit private val blockingExecutionContext = system.dispatchers.lookup("blocking-smtp-dispatcher")

  private val primaryClient   = new SMTPMailer(config.primary.toClientConfig)
  private val secondaryClient = new SMTPMailer(config.secondary.toClientConfig)

  private def randomClient(): SMTPMailer =
    if (ThreadLocalRandom.nextInt(1000) < getSecondaryPermille()) secondaryClient
    else primaryClient

  def send(msg: Mailer.Message): Funit =
    if (msg.to.isNoReply) {
      logger.warn(s"Can't send ${msg.subject} to noreply email ${msg.to}")
      funit
    } else
      Future {
        Chronometer.syncMon(_.email.send.time) {
          blocking {
            randomClient()
              .send(
                Email(
                  subject = msg.subject,
                  from = config.sender,
                  to = Seq(msg.to.value),
                  bodyText = msg.text.some,
                  bodyHtml = msg.htmlBody map { body => Mailer.html.wrap(msg.subject, body).render }
                )
              )
              .unit
          }
        }
      }
}

object Mailer {

  private val timeout = 5 seconds

  case class Smtp(
      mock: Boolean,
      host: String,
      port: Int,
      tls: Boolean,
      user: String,
      password: String
  ) {
    def toClientConfig = SMTPConfiguration(
      host = host,
      port = port,
      tlsRequired = tls,
      user = user.some,
      password = password.some,
      mock = mock,
      timeout = Mailer.timeout.toMillis.toInt.some
    )
  }
  implicit val smtpLoader = AutoConfig.loader[Smtp]

  case class Config(
      primary: Smtp,
      secondary: Smtp,
      sender: String
  )
  implicit val configLoader = AutoConfig.loader[Config]

  case class Message(
      to: EmailAddress,
      subject: String,
      text: String,
      htmlBody: Option[Frag] = none
  )

  object txt {

    def serviceNote(implicit lang: Lang): String = s"""
${trans.common_note("https://lichess.org").render}

${trans.common_contact("https://lichess.org/contact").render}"""
  }

  object html {

    val itemscope    = attr("itemscope").empty
    val itemtype     = attr("itemtype")
    val itemprop     = attr("itemprop")
    val emailMessage = div(itemscope, itemtype := "http://schema.org/EmailMessage")
    val pDesc        = p(itemprop := "description")
    val potentialAction =
      div(itemprop := "potentialAction", itemscope, itemtype := "http://schema.org/ViewAction")
    def metaName(cont: String) = meta(itemprop := "name", content := cont)
    val publisher              = div(itemprop := "publisher", itemscope, itemtype := "http://schema.org/Organization")
    val noteContact = a(itemprop := "url", href := "https://lichess.org/contact")(
      span(itemprop := "name")("lichess.org/contact")
    )

    def serviceNote(implicit lang: Lang) =
      publisher(
        small(
          trans.common_note(Mailer.html.noteLink),
          " ",
          trans.common_contact(noteContact),
          " ",
          lila.i18n.I18nKeys.readAboutOur(
            a(href := "https://lichess.org/privacy")(
              lila.i18n.I18nKeys.privacyPolicy()
            )
          )
        )
      )

    def standardEmail(body: String): Frag =
      emailMessage(
        pDesc(nl2brUnsafe(body)),
        publisher
      )

    val noteLink = a(
      itemprop := "url",
      href := "https://lichess.org/"
    )(span(itemprop := "name")("lichess.org"))

    def url(u: String)(implicit lang: Lang) =
      frag(
        meta(itemprop := "url", content := u),
        p(a(itemprop := "target", href := u)(u)),
        p(trans.common_orPaste(lang))
      )

    private[Mailer] def wrap(subject: String, body: Frag): Frag =
      frag(
        raw(s"""<!doctype html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
    <title>${escapeHtml(subject)}</title>
  </head>
  <body>"""),
        body,
        raw("""
  </body>
</html>""")
      )
  }
}
