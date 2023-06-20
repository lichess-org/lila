package lila.security

import scalatags.Text.all.*

import lila.common.config.*
import lila.common.EmailAddress
import lila.i18n.I18nKeys.{ emails as trans }
import lila.mailer.Mailer
import lila.user.{ User, UserRepo }

final class MagicLink(
    mailer: Mailer,
    userRepo: UserRepo,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
)(using Executor):

  import Mailer.html.*

  def send(user: User, email: EmailAddress): Funit =
    tokener make user.id flatMap { token =>
      lila.mon.email.send.magicLink.increment()
      val url                  = s"$baseUrl/auth/magic-link/login/$token"
      given play.api.i18n.Lang = user.realLang | lila.i18n.defaultLang
      mailer send Mailer.Message(
        to = email,
        subject = trans.logInToLichess.txt(user.username),
        text = Mailer.txt.addServiceNote(s"""
${trans.passwordReset_clickOrIgnore.txt()}

$url

${trans.common_orPaste.txt()}"""),
        htmlBody = emailMessage(
          p(trans.passwordReset_clickOrIgnore()),
          potentialAction(metaName("Log in"), Mailer.html.url(url)),
          serviceNote
        ).some
      )
    }

  def confirm(token: String): Fu[Option[User]] =
    tokener read token flatMapz userRepo.enabledById map {
      _.filter(_.canFullyLogin)
    }

  private val tokener = LoginToken.makeTokener(tokenerSecret, 10 minutes)

object MagicLink:

  import play.api.mvc.RequestHeader
  import lila.memo.RateLimit
  import lila.common.{ HTTPRequest, IpAddress }

  private lazy val rateLimitPerIP = RateLimit[IpAddress](
    credits = 5,
    duration = 1 hour,
    key = "login.magicLink.ip"
  )

  private lazy val rateLimitPerUser = RateLimit[UserId](
    credits = 3,
    duration = 1 hour,
    key = "login.magicLink.user"
  )

  private lazy val rateLimitPerEmail = RateLimit[String](
    credits = 3,
    duration = 1 hour,
    key = "login.magicLink.email"
  )

  def rateLimit[A](user: User, email: EmailAddress, req: RequestHeader, default: => Fu[A])(
      run: => Fu[A]
  ): Fu[A] =
    rateLimitPerUser(user.id, default):
      rateLimitPerEmail(email.value, default):
        rateLimitPerIP(HTTPRequest ipAddress req, default):
          run
