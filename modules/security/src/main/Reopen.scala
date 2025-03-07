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
  ): Fu[Either[(String, String), User]] =
    userRepo
      .enabledWithEmail(email.normalize)
      .flatMap:
        case Some(_) =>
          fuccess(Left("emailUsed" -> "This email address is already in use by an active account."))
        case _ =>
          userRepo
            .byId(u)
            .flatMap:
              case None =>
                fuccess(Left("noUser" -> "No account found with this username."))
              case Some(user) if user.enabled.yes =>
                fuccess(Left("alreadyActive" -> "This account is already active."))
              case Some(user) =>
                userRepo
                  .currentOrPrevEmail(user.id)
                  .flatMap:
                    case None =>
                      fuccess(
                        Left(
                          "noEmail" -> "That account doesn't have any associated email, and cannot be reopened."
                        )
                      )
                    case Some(prevEmail) if !email.similarTo(prevEmail) =>
                      fuccess(Left("differentEmail" -> "That account has a different email address."))
                    case _ =>
                      closedByMod(user).flatMap:
                        if _ then fuccess(Left("nope" -> "Sorry, that account can no longer be reopened."))
                        else
                          userRepo
                            .isForeverClosed(user)
                            .map:
                              if _ then
                                Left(
                                  "nope" -> "Sorry, but you explicitly requested that your account could never be reopened."
                                )
                              else Right(user)

  def send(user: User, email: EmailAddress)(using lang: Lang): Funit =
    tokener.make(user.id).flatMap { token =>
      lila.mon.email.send.reopen.increment()
      val url = s"$baseUrl/account/reopen/login/$token"
      mailer.send(
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
      )
    }

  def confirm(token: String): Fu[Option[User]] =
    tokener.read(token).flatMapz(userRepo.disabledById).flatMapz { user =>
      for _ <- userRepo.reopen(user.id)
      yield
        lila.common.Bus.pub(lila.core.security.ReopenAccount(user))
        user.some
    }

  private val tokener = StringToken.userId(tokenerSecret, 20.minutes)
