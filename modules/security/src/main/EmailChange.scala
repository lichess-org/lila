package lila.security

import play.api.i18n.Lang
import play.twirl.api.Html

import lila.common.EmailAddress
import lila.user.{ User, UserRepo }
import lila.i18n.I18nKeys.{ emails => trans }

final class EmailChange(
    mailgun: Mailgun,
    baseUrl: String,
    tokenerSecret: String
) {

  def send(user: User, email: EmailAddress)(implicit lang: Lang): Funit =
    tokener make TokenPayload(user.id, email).some flatMap { token =>
      lila.mon.email.change()
      val url = s"$baseUrl/account/email/confirm/$token"
      lila.log("auth").info(s"Change email URL ${user.username} $email $url")
      mailgun send Mailgun.Message(
        to = email,
        subject = trans.emailChange_subject.literalTxtTo(lang, List(user.username)),
        text = s"""
${trans.emailChange_intro.literalTxtTo(lang)}
${trans.emailChange_click.literalTxtTo(lang)}

$url

${trans.common_orPaste.literalTxtTo(lang)}

${Mailgun.txt.serviceNote}
""",
        htmlBody = Html(s"""
<div itemscope itemtype="http://schema.org/EmailMessage">
  <p itemprop="description">${trans.emailChange_intro.literalHtmlTo(lang)}</p>
  <p>${trans.emailChange_click.literalHtmlTo(lang)}</p>
  <div itemprop="potentialAction" itemscope itemtype="http://schema.org/ViewAction">
    <meta itemprop="name" content="Change email address">
    ${Mailgun.html.url(url)}
  </div>
  ${Mailgun.html.serviceNote}
</div>""").some
      )
    }

  def confirm(token: String): Fu[Option[User]] =
    tokener read token map (_.flatten) flatMap {
      _ ?? {
        case TokenPayload(userId, email) =>
          UserRepo.email(userId, email).nevermind >> UserRepo.byId(userId)
      }
    }

  case class TokenPayload(userId: User.ID, email: EmailAddress)

  private implicit final val payloadSerializable = new StringToken.Serializable[Option[TokenPayload]] {
    private val sep = ' '
    def read(str: String) = str.split(sep) match {
      case Array(id, email) => EmailAddress from email map { TokenPayload(id, _) }
      case _ => none
    }
    def write(a: Option[TokenPayload]) = a ?? {
      case TokenPayload(userId, email) => s"$userId$sep$email"
    }
  }

  private val tokener = new StringToken[Option[TokenPayload]](
    secret = tokenerSecret,
    getCurrentValue = p => p ?? {
      case TokenPayload(userId, _) => UserRepo email userId map (_.??(_.value))
    }
  )
}
