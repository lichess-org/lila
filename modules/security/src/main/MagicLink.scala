package lila.security

import scala.concurrent.duration._
import scalatags.Text.all._

import lila.common.config._
import lila.common.EmailAddress
import lila.i18n.I18nKeys.{ emails => trans }
import lila.user.{ User, UserRepo }

final class MagicLink(
    mailgun: Mailgun,
    userRepo: UserRepo,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Mailgun.html._

  def send(user: User, email: EmailAddress): Funit =
    tokener make user.id flatMap { token =>
      lila.mon.email.send.magicLink.increment()
      val url           = s"$baseUrl/auth/magic-link/login/$token"
      implicit val lang = user.realLang | lila.i18n.defaultLang
      mailgun send Mailgun.Message(
        to = email,
        subject = trans.logInToLichess.txt(user.username),
        text = s"""
${trans.passwordReset_clickOrIgnore.txt()}

$url

${trans.common_orPaste.txt()}

${Mailgun.txt.serviceNote}
""",
        htmlBody = emailMessage(
          p(trans.passwordReset_clickOrIgnore()),
          potentialAction(metaName("Log in"), Mailgun.html.url(url)),
          serviceNote
        ).some
      )
    }

  def confirm(token: String): Fu[Option[User]] =
    tokener read token flatMap { _ ?? userRepo.byId } map {
      _.filter(_.canLogin)
    }

  private val tokener = LoginToken.makeTokener(tokenerSecret, 10 minutes)
}

object MagicLink {

  import scala.concurrent.duration._
  import play.api.mvc.RequestHeader
  import ornicar.scalalib.Zero
  import lila.memo.RateLimit
  import lila.common.{ HTTPRequest, IpAddress }

  private lazy val rateLimitPerIP = new RateLimit[IpAddress](
    credits = 5,
    duration = 1 hour,
    key = "login.magicLink.ip"
  )

  private lazy val rateLimitPerUser = new RateLimit[String](
    credits = 3,
    duration = 1 hour,
    key = "login.magicLink.user"
  )

  private lazy val rateLimitPerEmail = new RateLimit[String](
    credits = 3,
    duration = 1 hour,
    key = "login.magicLink.email"
  )

  def rateLimit[A: Zero](user: User, email: EmailAddress, req: RequestHeader)(
      run: => Fu[A]
  )(default: => Fu[A]): Fu[A] =
    rateLimitPerUser(user.id, cost = 1) {
      rateLimitPerEmail(email.value, cost = 1) {
        rateLimitPerIP(HTTPRequest ipAddress req, cost = 1) {
          run
        }(default)
      }(default)
    }(default)
}
