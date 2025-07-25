package lila.security

import play.api.i18n.Lang
import scalatags.Text.all.*

import lila.core.config.*
import lila.core.i18n.I18nKey.emails as trans
import lila.mailer.Mailer
import lila.user.{ Me, User, UserRepo }
import lila.core.net.IpAddress
import lila.memo.RateLimit

final class PasswordReset(
    mailer: Mailer,
    userRepo: UserRepo,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
)(using Executor, lila.core.i18n.Translator, lila.core.config.RateLimit):

  import Mailer.html.*

  def send(user: User, email: EmailAddress)(using lang: Lang): Funit =
    tokener.make(user.id).flatMap { token =>
      lila.mon.email.send.resetPassword.increment()
      val url = s"$baseUrl/password/reset/confirm/$token"
      mailer.sendOrFail:
        Mailer.Message(
          to = email,
          subject = trans.passwordReset_subject.txt(user.username),
          text = Mailer.txt.addServiceNote(s"""
${trans.passwordReset_intro.txt()}

${trans.passwordReset_clickOrIgnore.txt()}

$url

${trans.common_orPaste.txt()}"""),
          htmlBody = emailMessage(
            pDesc(trans.passwordReset_intro()),
            p(trans.passwordReset_clickOrIgnore()),
            potentialAction(metaName("Reset password"), Mailer.html.url(url)),
            serviceNote
          ).some
        )
    }

  def confirm(token: String): Fu[Option[Me]] =
    tokener.read(token).flatMapz(userRepo.me).map(_.filter(Granter.canFullyLogin))

  val limiter: RateLimit.RateLimiter[(EmailAddress, IpAddress)] = RateLimit.combine(
    RateLimit[EmailAddress](credits = 3, duration = 1.day, key = "password.reset.email"),
    RateLimit[IpAddress](credits = 20, duration = 1.day, key = "password.reset.ip")
  )

  private val tokener = StringToken.withLifetimeAndFutureValue[UserId](
    secret = tokenerSecret,
    lifetime = 10.minutes,
    getCurrentValue = id =>
      for
        hash <- userRepo.getPasswordHash(id)
        email <- userRepo.email(id)
      yield ~hash + email.so(_.value)
  )
