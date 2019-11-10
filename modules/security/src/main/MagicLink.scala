package lila.security

import scala.concurrent.duration._
import scalatags.Text.all._

import lila.common.{ Lang, EmailAddress }
import lila.i18n.I18nKeys.{ emails => trans }
import lila.user.{ User, UserRepo }

final class MagicLink(
    mailgun: Mailgun,
    baseUrl: String,
    tokenerSecret: String
) {

  import Mailgun.html._

  def send(user: User, email: EmailAddress)(implicit lang: Lang): Funit =
    tokener make user.id flatMap { token =>
      lila.mon.email.types.magicLink()
      val url = s"$baseUrl/auth/magic-link/login/$token"
      mailgun send Mailgun.Message(
        to = email,
        subject = "Log in to lichess.org",
        text = s"""
${trans.passwordReset_clickOrIgnore.literalTxtTo(lang)}

$url

${trans.common_orPaste.literalTxtTo(lang)}

${Mailgun.txt.serviceNote}
""",
        htmlBody = emailMessage(
          p(trans.passwordReset_clickOrIgnore.literalTo(lang)),
          potentialAction(metaName("Log in"), Mailgun.html.url(url)),
          serviceNote
        ).some
      )
    }

  def confirm(token: String): Fu[Option[User]] =
    tokener read token flatMap { _ ?? UserRepo.byId }

  private val tokener = LoginToken.makeTokener(tokenerSecret, 10 minutes)
}

object MagicLink {

  import scala.concurrent.duration._
  import play.api.mvc.RequestHeader
  import ornicar.scalalib.Zero
  import lila.memo.RateLimit
  import lila.common.{ IpAddress, HTTPRequest }

  private lazy val rateLimitPerIP = new RateLimit[IpAddress](
    credits = 5,
    duration = 1 hour,
    name = "Magic links per IP",
    key = "email.confirms.ip"
  )

  private lazy val rateLimitPerUser = new RateLimit[String](
    credits = 3,
    duration = 1 hour,
    name = "Magic links per user",
    key = "email.confirms.user"
  )

  private lazy val rateLimitPerEmail = new RateLimit[String](
    credits = 3,
    duration = 1 hour,
    name = "Magic links per email",
    key = "email.confirms.email"
  )

  def rateLimit[A: Zero](user: User, email: EmailAddress, req: RequestHeader)(run: => Fu[A]): Fu[A] =
    rateLimitPerUser(user.id, cost = 1) {
      rateLimitPerEmail(email.value, cost = 1) {
        rateLimitPerIP(HTTPRequest lastRemoteAddress req, cost = 1) {
          run
        }
      }
    }
}
