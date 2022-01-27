package lila.security

import play.api.i18n.Lang
import scala.concurrent.duration._
import scalatags.Text.all._

import lila.common.config._
import lila.common.EmailAddress
import lila.i18n.I18nKeys.{ emails => trans }
import lila.mailer.Mailer
import lila.user.{ User, UserRepo }

final class Reopen(
    mailer: lila.mailer.Mailer,
    userRepo: UserRepo,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Mailer.html._

  def prepare(
      username: String,
      email: EmailAddress,
      closedByMod: User => Fu[Boolean]
  ): Fu[Either[(String, String), User]] =
    userRepo.enabledWithEmail(email.normalize) flatMap {
      case Some(_) =>
        fuccess(Left("emailUsed" -> "This email address is already in use by an active account."))
      case _ =>
        val userId = User normalize username
        userRepo.byIdNotErased(userId) flatMap {
          case None =>
            fuccess(Left("noUser" -> "No account found with this username."))
          case Some(user) if user.enabled =>
            fuccess(Left("alreadyActive" -> "This account is already active."))
          case Some(user) =>
            userRepo.currentOrPrevEmail(user.id) flatMap {
              case None =>
                fuccess(
                  Left("noEmail" -> "That account doesn't have any associated email, and cannot be reopened.")
                )
              case Some(prevEmail) if !email.similarTo(prevEmail) =>
                fuccess(Left("differentEmail" -> "That account has a different email address."))
              case _ =>
                closedByMod(user) map {
                  case true => Left("nope" -> "Sorry, that account can no longer be reopened.")
                  case _    => Right(user)
                }
            }
        }
    }

  def send(user: User, email: EmailAddress)(implicit lang: Lang): Funit =
    tokener make user.id flatMap { token =>
      lila.mon.email.send.reopen.increment()
      val url = s"$baseUrl/account/reopen/login/$token"
      mailer send Mailer.Message(
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
    tokener read token flatMap { _ ?? userRepo.disabledById } flatMap {
      _ ?? { user =>
        userRepo reopen user.id inject user.some
      }
    }

  private val tokener = LoginToken.makeTokener(tokenerSecret, 20 minutes)
}
