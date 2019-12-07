package lila.security

import scala.concurrent.duration.{ span => _, _ }
import play.api.i18n.Lang
import akka.actor.ActorSystem
import io.methvin.play.autoconfig._
import play.api.libs.ws.{ WSClient, WSAuthScheme }
import scalatags.Text.all._

import lila.common.config.Secret
import lila.common.String.html.{ escapeHtml, nl2brUnsafe }
import lila.common.EmailAddress
import lila.i18n.I18nKeys.{ emails => trans }

final class Mailgun(
    ws: WSClient,
    config: Mailgun.Config
)(implicit system: ActorSystem) {

  def send(msg: Mailgun.Message): Funit =
    if (config.apiUrl.isEmpty) {
      println(msg, "No mailgun API URL")
      funit
    } else {
      lila.mon.email.actions.send()
      ws.url(s"${config.apiUrl}/messages")
        .withAuth("api", config.apiKey.value, WSAuthScheme.BASIC).post(Map(
          "from" -> Seq(msg.from | config.sender),
          "to" -> Seq(msg.to.value),
          "h:Reply-To" -> Seq(msg.replyTo | config.replyTo),
          "o:tag" -> msg.tag.toSeq,
          "subject" -> Seq(msg.subject),
          "text" -> Seq(msg.text)
        ) ++ msg.htmlBody.?? { body =>
            Map("html" -> Seq(Mailgun.html.wrap(msg.subject, body).render))
          }).void addFailureEffect {
          case e: java.net.ConnectException => lila.mon.http.mailgun.timeout()
          case _ =>
        } recoverWith {
          case e if msg.retriesLeft > 0 => {
            lila.mon.email.actions.retry()
            akka.pattern.after(15 seconds, system.scheduler) {
              send(msg.copy(retriesLeft = msg.retriesLeft - 1))
            }
          }
          case e => {
            lila.mon.email.actions.fail()
            fufail(e)
          }
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
${trans.common_note.literalTo(lang, List("https://lichess.org")).render}

${trans.common_contact.literalTo(lang, List("https://lichess.org/contact")).render}"""
  }

  object html {

    val itemscope = attr("itemscope").empty
    val itemtype = attr("itemtype")
    val itemprop = attr("itemprop")
    val emailMessage = div(itemscope, itemtype := "http://schema.org/EmailMessage")
    val pDesc = p(itemprop := "description")
    val potentialAction = div(itemprop := "potentialAction", itemscope, itemtype := "http://schema.org/ViewAction")
    def metaName(cont: String) = meta(itemprop := "name", content := cont)
    val publisher = div(itemprop := "publisher", itemscope, itemtype := "http://schema.org/Organization")
    val noteContact = a(itemprop := "url", href := "https://lichess.org/contact")(
      span(itemprop := "name")("lichess.org/contact")
    )

    def serviceNote(implicit lang: Lang) = publisher(
      small(
        trans.common_note.literalTo(lang, List(Mailgun.html.noteLink)),
        " ",
        trans.common_contact.literalTo(lang, List(noteContact))
      )
    )

    def standardEmail(body: String)(implicit lang: Lang): Frag =
      emailMessage(
        pDesc(nl2brUnsafe(body)),
        publisher
      )

    val noteLink = a(
      itemprop := "url",
      href := "https://lichess.org/"
    )(span(itemprop := "name")("lichess.org"))

    def url(u: String)(implicit lang: Lang) = frag(
      meta(itemprop := "url", content := u),
      p(a(itemprop := "target", href := u)(u)),
      p(trans.common_orPaste.literalTo(lang))
    )

    private[Mailgun] def wrap(subject: String, body: Frag): Frag = frag(
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
