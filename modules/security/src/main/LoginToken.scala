package lila.security

import scalatags.Text.all.*

import lila.core.config.*
import lila.core.i18n.I18nKey.emails as trans
import lila.mailer.Mailer
import lila.user.{ User, UserRepo }

final class LoginToken(
    mailer: Mailer,
    userRepo: UserRepo,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
)(using Executor, lila.core.i18n.Translator, lila.core.config.RateLimit):

  private val tokener = StringToken.withLifetime[UserId](tokenerSecret, 10.minutes)

  def generate[U: UserIdOf](user: U): Fu[String] = tokener.make(user.id)

  def send(user: User, email: EmailAddress): Funit =
    generate(user).flatMap { token =>
      lila.mon.email.send.magicLink.increment()
      val url = s"$baseUrl/auth/token/$token"
      given play.api.i18n.Lang = user.realLang | lila.core.i18n.defaultLang
      import Mailer.html.*
      mailer.sendOrFail:
        Mailer.Message(
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

  def consume(token: String): Fu[Option[User]] =
    tokener.read(token).flatMapz(userRepo.enabledById).map(_.filter(Granter.canFullyLogin))

  object rateLimit:

    import play.api.mvc.RequestHeader
    import lila.memo.RateLimit
    import lila.common.HTTPRequest
    import lila.core.net.IpAddress

    private lazy val rateLimitPerIP = RateLimit[IpAddress](
      credits = 5,
      duration = 1.hour,
      key = "login.magicLink.ip"
    )

    private lazy val rateLimitPerUser = RateLimit[UserId](
      credits = 3,
      duration = 1.hour,
      key = "login.magicLink.user"
    )

    private lazy val rateLimitPerEmail = RateLimit[String](
      credits = 3,
      duration = 1.hour,
      key = "login.magicLink.email"
    )

    def apply[A](user: User, email: EmailAddress, req: RequestHeader, default: => Fu[A])(
        run: => Fu[A]
    ): Fu[A] =
      rateLimitPerUser(user.id, default):
        rateLimitPerEmail(email.value, default):
          rateLimitPerIP(HTTPRequest.ipAddress(req), default):
            run
