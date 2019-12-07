package lila.security

import scalatags.Text.all._
import play.api.i18n.Lang
import lila.common.config._
import lila.common.EmailAddress
import lila.i18n.I18nKeys.{ emails => trans }
import lila.user.{ User, UserRepo }

final class EmailChange(
    userRepo: UserRepo,
    mailgun: Mailgun,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
) {

  import Mailgun.html._

  def send(user: User, email: EmailAddress)(implicit lang: Lang): Funit =
    tokener make TokenPayload(user.id, email).some flatMap { token =>
      lila.mon.email.types.change()
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
        htmlBody = emailMessage(
          pDesc(trans.emailChange_intro.literalTo(lang)),
          p(trans.emailChange_click.literalTo(lang)),
          potentialAction(metaName("Change email address"), Mailgun.html.url(url)),
          serviceNote
        ).some
      )
    }

  def confirm(token: String): Fu[Option[User]] =
    tokener read token map (_.flatten) flatMap {
      _ ?? {
        case TokenPayload(userId, email) =>
          userRepo.setEmail(userId, email).nevermind >> userRepo.byId(userId)
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
      case TokenPayload(userId, EmailAddress(email)) => s"$userId$sep$email"
    }
  }

  private val tokener = new StringToken[Option[TokenPayload]](
    secret = tokenerSecret,
    getCurrentValue = p => p ?? {
      case TokenPayload(userId, _) => userRepo email userId map (_.??(_.value))
    }
  )
}
