package lila.security

import play.api.i18n.Lang
import scalatags.Text.all.*

import lila.core.config.*
import lila.core.i18n.I18nKey.emails as trans
import lila.mailer.Mailer
import lila.user.{ User, UserRepo }

final class Reopen(
    mailer: lila.mailer.Mailer,
    userRepo: UserRepo,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
)(using Executor, lila.core.i18n.Translator):

  import Mailer.html.*

  def prepare(
      u: UserStr,
      email: EmailAddress,
      closedByMod: User => Fu[Boolean]
  ): FuRaise[(String, String), User] = for
    existing <- userRepo.enabledWithEmail(email.normalize)
    _ <- raiseIf(existing.isDefined):
      "emailUsed" -> "This email address is already in use by an active account."
    user <- userRepo.byId(u)
    user <- user.raiseIfNone("noUser" -> "No account found with this username.")
    _ <- raiseIf(user.enabled.yes):
      "alreadyActive" -> "This account is already active."
    userEmail <- userRepo.currentOrPrevEmail(user.id)
    userEmail <- userEmail.raiseIfNone:
      "noEmail" -> "That account doesn't have any associated email, and cannot be reopened."
    _ <- raiseIf(!email.similarTo(userEmail)):
      "differentEmail" -> "That account has a different email address."
    modClosed <- closedByMod(user)
    _ <- raiseIf(modClosed):
      "nope" -> "Sorry, that account can no longer be reopened."
    forever <- userRepo.isForeverClosed(user)
    _ <- raiseIf(forever):
      "nope" -> "Sorry, but you explicitly requested that your account could never be reopened."
  yield user

  def send(user: User, email: EmailAddress)(using lang: Lang): Funit =
    tokener.make(user.id).flatMap { token =>
      lila.mon.email.send.reopen.increment()
      val url = s"$baseUrl/account/reopen/login/$token"
      mailer.sendOrFail:
        Mailer.Message(
          to = email,
          subject = s"Reopen your lichess.org account: ${user.username}",
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
    tokener.read(token).flatMapz(userRepo.disabledById).flatMapz { user =>
      for
        forever <- userRepo.isForeverClosed(user)
        _ <- forever.not.so(userRepo.reopen(user.id))
        reopened = forever.not.option(user)
      yield
        if reopened.isDefined
        then lila.common.Bus.pub(lila.core.security.ReopenAccount(user))
        reopened
    }

  private val tokener = StringToken.withLifetime[UserId](tokenerSecret, 20.minutes)
