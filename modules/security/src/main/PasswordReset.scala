package lila.security

import play.twirl.api.Html

import lila.common.{ Lang, EmailAddress }
import lila.user.{ User, UserRepo }
import lila.i18n.I18nKeys.{ emails => trans }

final class PasswordReset(
    mailgun: Mailgun,
    baseUrl: String,
    tokenerSecret: String
) {

  def send(user: User, email: EmailAddress)(implicit lang: Lang): Funit =
    tokener make user.id flatMap { token =>
      lila.mon.email.resetPassword()
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
        htmlBody = Html(s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">${trans.passwordReset_intro.literalHtmlTo(lang)}</p>
  <p>${trans.passwordReset_clickOrIgnore.literalHtmlTo(lang)}</p>
  <div itemprop="potentialAction" itemscope itemtype="http://schema.org/ViewAction">
    <meta itemprop="name" content="Reset password">
    ${Mailgun.html.url(url)}
  </div>
  ${Mailgun.html.serviceNote}
</div>""").some
      )
    }

  def confirm(token: String): Fu[Option[User]] =
    tokener read token flatMap { _ ?? UserRepo.byId }

  private val tokener = new StringToken[User.ID](
    secret = tokenerSecret,
    getCurrentValue = id => UserRepo getPasswordHash id map (~_)
  )
}
