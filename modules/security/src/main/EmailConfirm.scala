package lila.security

import play.api.i18n.Lang
import play.twirl.api.Html

import lila.common.EmailAddress
import lila.i18n.I18nKeys.{ emails => trans }
import lila.user.{ User, UserRepo }

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
    lila.log("auth").info(s"Confirm URL ${user.username} $email $url")
    mailgun send Mailgun.Message(
      to = email,
      subject = trans.emailConfirm_subject.literalTxtTo(lang, List(user.username)),
      text = s"""
${trans.emailConfirm_click.literalTxtTo(lang)}

$url

${trans.common_orPaste.literalTxtTo(lang)}

${Mailgun.txt.serviceNote}
${trans.emailConfirm_ignore.literalTxtTo(lang, List("https://lichess.org"))}
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

object EmailConfirm {

  case class UserEmail(username: String, email: EmailAddress)

  object cookie {

    import play.api.mvc.{ Cookie, RequestHeader }

    val name = "email_confirm"
    val sep = ":"

    def get(req: RequestHeader): Option[UserEmail] = req.session get name map (_.split(sep, 2)) collect {
      case Array(username, email) => UserEmail(username, EmailAddress(email))
    }

    def make(user: User, email: EmailAddress)(implicit req: RequestHeader): Cookie = lila.common.LilaCookie.session(
      name = name,
      value = s"${user.username}$sep${email.value}"
    )
  }

  import scala.concurrent.duration._
  import play.api.mvc.RequestHeader
  import ornicar.scalalib.Zero
  import lila.memo.RateLimit
  import lila.common.{ IpAddress, HTTPRequest }

  private lazy val rateLimitPerIP = new RateLimit[IpAddress](
    credits = 30,
    duration = 1 hour,
    name = "Confirm emails per IP",
    key = "email.confirms.ip"
  )

  private lazy val rateLimitPerUser = new RateLimit[String](
    credits = 3,
    duration = 1 hour,
    name = "Confirm emails per user",
    key = "email.confirms.user"
  )

  private lazy val rateLimitPerEmail = new RateLimit[String](
    credits = 3,
    duration = 1 hour,
    name = "Confirm emails per email",
    key = "email.confirms.email"
  )

  def rateLimit[A: Zero](userEmail: UserEmail, req: RequestHeader)(run: => Fu[A]): Fu[A] =
    rateLimitPerUser(userEmail.username, cost = 1) {
      rateLimitPerEmail(userEmail.email.value, cost = 1) {
        rateLimitPerIP(HTTPRequest lastRemoteAddress req, cost = 1) {
          run
        }
      }
    }
}
