package lila.security

import play.api.i18n.Lang
import lila.common.EmailAddress
import lila.user.{ User, UserRepo }
import lila.i18n.I18nKeys.{ emails => trans }

trait EmailConfirm {

  def effective: Boolean

  def send(user: User, email: EmailAddress)(implicit lang: Lang): Funit

  def confirm(token: String): Fu[Option[User]]
}

object EmailConfirmSkip extends EmailConfirm {

  def effective = false

  def send(user: User, email: EmailAddress)(implicit lang: Lang) = UserRepo setEmailConfirmed user.id

  def confirm(token: String): Fu[Option[User]] = fuccess(none)
}

final class EmailConfirmMailgun(
    mailgun: Mailgun,
    baseUrl: String,
    tokenerSecret: String
) extends EmailConfirm {

  def effective = true

  val maxTries = 3

  def send(user: User, email: EmailAddress)(implicit lang: Lang): Funit = tokener make user.id flatMap { token =>
    lila.mon.email.confirmation()
    val url = s"$baseUrl/signup/confirm/$token"
    mailgun send Mailgun.Message(
      to = email,
      subject = trans.confirm_subject.literalTxtTo(lang, List(user.username)),
      text = s"""
${trans.confirm_click.literalTxtTo(lang)}

$url

${trans.confirm_orPaste.literalTxtTo(lang)}

${Mailgun.txt.serviceNote}
${trans.confirm_ignore.literalTxtTo(lang, List("https://lichess.org"))}
""",
      htmlBody = s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">${trans.confirm_click.literalHtmlTo(lang)}</p>
  <div itemprop="potentialAction" itemscope itemtype="http://schema.org/ViewAction">
    <meta itemprop="name" content="Activate account">
    ${Mailgun.html.url(url)}
  </div>
  <div itemprop="publisher" itemscope itemtype="http://schema.org/Organization">
    <small>
      ${Mailgun.html.serviceNote}
      ${trans.confirm_orPaste.literalTxtTo(lang)}
    </small>
  </div>
  ${Mailgun.html.serviceNote}
</div>""".some
    )
  }

  def confirm(token: String): Fu[Option[User]] = tokener read token flatMap {
    _ ?? { userId =>
      UserRepo.mustConfirmEmail(userId) flatMap {
        _ ?? {
          (UserRepo setEmailConfirmed userId) >> (UserRepo byId userId)
        }
      }
    }
  }

  private val tokener = new StringToken[User.ID](
    secret = tokenerSecret,
    getCurrentValue = id => UserRepo email id map (_.??(_.value))
  )
}
