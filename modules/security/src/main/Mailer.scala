package lila.security

import akka.actor.ActorSystem
import io.methvin.play.autoconfig._
import play.api.i18n.Lang
import play.api.libs.mailer.{ Email, MailerClient }
import scala.concurrent.duration._
import scala.concurrent.{ blocking, Future }
import scalatags.Text.all.{ span => spanTag, _ }

import lila.common.config.Secret
import lila.common.{ Chronometer, EmailAddress }
import lila.common.String.html.{ escapeHtml, nl2brUnsafe }
import lila.i18n.I18nKeys.{ emails => trans }

final class Mailer(
    client: MailerClient,
    config: Mailer.Config
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  private val workQueue =
    new lila.hub.DuctSequencer(maxSize = 512, timeout = Mailer.timeout * 2, name = "mailer")

  def send(msg: Mailer.Message): Funit =
    if (msg.to.isNoReply) {
      logger.warn(s"Can't send ${msg.subject} to noreply email ${msg.to}")
      funit
    } else
      workQueue {
        Future {
          Chronometer.syncMon(_.email.send.time) {
            blocking {
              client
                .send(
                  Email(
                    subject = msg.subject,
                    from = config.sender,
                    replyTo = Seq(config.sender),
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
}

object Mailer {

  val timeout = 10 seconds

  case class Config(
      @ConfigName("smtp.mock") smtpMock: Boolean,
      @ConfigName("smtp.host") smtpHost: String,
      @ConfigName("smtp.port") smtpPort: Int,
      @ConfigName("smtp.user") smtpUser: String,
      @ConfigName("smtp.tls") smtpTls: Boolean,
      @ConfigName("smtp.password") smtpPassword: String,
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
      spanTag(itemprop := "name")("lichess.org/contact")
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
    )(spanTag(itemprop := "name")("lichess.org"))

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
