package lila.security

import scala.concurrent.duration._

import akka.actor.ActorSystem
import play.api.libs.ws.{ WS, WSAuthScheme }
import play.twirl.api.Html
import play.api.Play.current

import lila.common.EmailAddress

final class Mailgun(
    apiUrl: String,
    apiKey: String,
    from: String,
    replyTo: String,
    system: ActorSystem,
    maxTries: Int = 3
) {

  def send(msg: Mailgun.Message): Funit =
    WS.url(s"$apiUrl/messages").withAuth("api", apiKey, WSAuthScheme.BASIC).post(Map(
      "from" -> Seq(msg.from | from),
      "to" -> Seq(msg.to.value),
      "h:Reply-To" -> Seq(msg.replyTo | replyTo),
      "o:tag" -> msg.tag.toSeq,
      "subject" -> Seq(msg.subject),
      "text" -> Seq(msg.text)
    ) ++ msg.htmlBody.?? { body =>
        Map("html" -> Seq(makeHtml(msg.subject, body)))
      }).void addFailureEffect {
      case e: java.net.ConnectException => lila.mon.http.mailgun.timeout()
      case _ =>
    } recoverWith {
      case e if msg.tryNb < maxTries => akka.pattern.after(15 seconds, system.scheduler) {
        send(msg.copy(tryNb = msg.tryNb + 1))
      }
      case e => fufail(e)
    }

  private def makeHtml(subject: String, body: String) = s"""<!doctype html>
<html>
  <head>
    <meta http-equiv="Content-Type" content="text/html; charset=utf-8" />
    <meta name="viewport" content="width=device-width" />
    <title>$subject</title>
  </head>
  <body>
    $body
  </body>
</html>"""
}

object Mailgun {

  case class Message(
    to: EmailAddress,
    subject: String,
    text: String,
    htmlBody: Option[String] = none,
    from: Option[String] = none,
    replyTo: Option[String] = none,
    tag: Option[String] = none,
    tryNb: Int = 1
  )
}
