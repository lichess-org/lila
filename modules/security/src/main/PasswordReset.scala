package lila.security

import scalatags.Text.all._

import lila.common.{ Lang, EmailAddress }
import lila.i18n.I18nKeys.{ emails => trans }
import lila.user.{ User, UserRepo }

final class PasswordReset(
    mailgun: Mailgun,
    baseUrl: String,
    tokenerSecret: String
) {

  import Mailgun.html._

  def send(user: User, email: EmailAddress)(implicit lang: Lang): Funit =
    tokener make user.id flatMap { token =>
      lila.mon.email.types.resetPassword()
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
    tokener read token flatMap { _ ?? UserRepo.byId }

  private val tokener = new StringToken[User.ID](
    secret = tokenerSecret,
    getCurrentValue = id => UserRepo getPasswordHash id map (~_)
  )
}
