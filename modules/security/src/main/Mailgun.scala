package lila.security

import akka.actor.ActorSystem
import io.methvin.play.autoconfig._
import play.api.i18n.Lang
import play.api.libs.ws.DefaultBodyWritables._
import play.api.libs.ws.{ StandaloneWSClient, WSAuthScheme }
import scala.concurrent.duration.{ span => _, _ }
import scalatags.Text.all._

import lila.common.config.Secret
import lila.common.EmailAddress
import lila.common.String.html.{ escapeHtml, nl2brUnsafe }
import lila.i18n.I18nKeys.{ emails => trans }

final class Mailgun(
    ws: StandaloneWSClient,
    config: Mailgun.Config
)(implicit
    ec: scala.concurrent.ExecutionContext,
    system: ActorSystem
) {

  def send(msg: Mailgun.Message): Funit =
    if (config.apiUrl.isEmpty) {
      logger.info(s"$msg -> No mailgun API URL")
      funit
    } else if (msg.to.isNoReply) {
      logger.warn(s"Can't send ${msg.subject} to noreply email ${msg.to}")
      funit
    } else
      ws.url(s"${config.apiUrl}/messages")
        .withAuth("api", config.apiKey.value, WSAuthScheme.BASIC)
        .post(
          Map(
            "from"       -> Seq(msg.from | config.sender),
            "to"         -> Seq(msg.to.value),
            "h:Reply-To" -> Seq(msg.replyTo | config.replyTo),
            "o:tag"      -> msg.tag.toSeq,
            "subject"    -> Seq(msg.subject),
            "text"       -> Seq(msg.text)
          ) ++ msg.htmlBody.?? { body =>
            Map("html" -> Seq(Mailgun.html.wrap(msg.subject, body).render))
          }
        )
        .addFailureEffect {
          case _: java.net.ConnectException => lila.mon.email.send.error("timeout").increment().unit
          case _                            =>
        }
        .flatMap {
          case res if res.status >= 300 =>
            lila.mon.email.send.error(res.status.toString).increment()
            fufail(s"""Can't send to mailgun: ${res.status} to: "${msg.to.value}" ${res.body take 500}""")
          case _ => funit
        }
        .mon(_.email.send.time)
        .recoverWith {
          case _ if msg.retriesLeft > 0 =>
            akka.pattern.after(15 seconds, system.scheduler) {
              send(msg.copy(retriesLeft = msg.retriesLeft - 1))
            }
        }
}

object Mailgun {

  case class Config(
      @ConfigName("api.url") apiUrl: String,
      @ConfigName("api.key") apiKey: Secret,
      sender: String,
      @ConfigName("reply_to") replyTo: String
  )
  implicit val configLoader = AutoConfig.loader[Config]

  case class Message(
      to: EmailAddress,
      subject: String,
      text: String,
      htmlBody: Option[Frag] = none,
      from: Option[String] = none,
      replyTo: Option[String] = none,
      tag: Option[String] = none,
      retriesLeft: Int = 3
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
          trans.common_note(Mailgun.html.noteLink),
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

    private[Mailgun] def wrap(subject: String, body: Frag): Frag =
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
