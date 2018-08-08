package lidraughts.security

import play.api.i18n.Lang
import play.twirl.api.Html

import lidraughts.common.EmailAddress
import lidraughts.user.{ User, UserRepo }
import lidraughts.i18n.I18nKeys.{ emails => trans }

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
    lidraughts.mon.email.confirmation()
    val url = s"$baseUrl/signup/confirm/$token"
    lidraughts.log("auth").info(s"Confirm URL ${user.username} $email $url")
    mailgun send Mailgun.Message(
      to = email,
      subject = trans.emailConfirm_subject.literalTxtTo(lang, List(user.username)),
      text = s"""
${trans.emailConfirm_click.literalTxtTo(lang)}

$url

${trans.common_orPaste.literalTxtTo(lang)}

${Mailgun.txt.serviceNote}
${trans.emailConfirm_ignore.literalTxtTo(lang, List("https://lidraughts.org"))}
""",
      htmlBody = Html(s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">${trans.emailConfirm_click.literalHtmlTo(lang)}</p>
  <div itemprop="potentialAction" itemscope itemtype="http://schema.org/ViewAction">
    <meta itemprop="name" content="Activate account">
    ${Mailgun.html.url(url)}
  </div>
  <div itemprop="publisher" itemscope itemtype="http://schema.org/Organization">
    <small>
      ${trans.common_note.literalHtmlTo(lang, List(Mailgun.html.noteLink))}
      ${trans.emailConfirm_ignore.literalHtmlTo(lang)}
    </small>
  </div>
</div>""").some
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
