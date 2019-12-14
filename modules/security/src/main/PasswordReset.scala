package lila.security

import play.api.i18n.Lang
import scalatags.Text.all._

import lila.common.config._
import lila.common.EmailAddress
import lila.i18n.I18nKeys.{ emails => trans }
import lila.user.{ User, UserRepo }

final class PasswordReset(
    mailgun: Mailgun,
    userRepo: UserRepo,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
)(implicit ec: scala.concurrent.ExecutionContext) {

  import Mailgun.html._

  def send(user: User, email: EmailAddress)(implicit lang: Lang): Funit =
    tokener make user.id flatMap { token =>
      lila.mon.email.send.resetPassword.increment()
      val url = s"$baseUrl/password/reset/confirm/$token"
      mailgun send Mailgun.Message(
        to = email,
        subject = trans.passwordReset_subject.literalTxtTo(lang, List(user.username)),
        text = s"""
${trans.passwordReset_intro.literalTxtTo(lang)}

${trans.passwordReset_clickOrIgnore.literalTxtTo(lang)}

$url

${trans.common_orPaste.literalTxtTo(lang)}

${Mailgun.txt.serviceNote}
""",
        htmlBody = emailMessage(
          pDesc(trans.passwordReset_intro.literalTo(lang)),
          p(trans.passwordReset_clickOrIgnore.literalTo(lang)),
          potentialAction(metaName("Reset password"), Mailgun.html.url(url)),
          serviceNote
        ).some
      )
    }

  def confirm(token: String): Fu[Option[User]] =
    tokener read token flatMap { _ ?? userRepo.byId }

  private val tokener = new StringToken[User.ID](
    secret = tokenerSecret,
    getCurrentValue = id => userRepo getPasswordHash id dmap (~_)
  )
}
