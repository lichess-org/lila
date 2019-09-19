package lila.security

import scalatags.Text.all._

import lila.common.{ Lang, EmailAddress }
import lila.i18n.I18nKeys.{ emails => trans }
import lila.user.{ User, UserRepo }

trait EmailConfirm {

  def effective: Boolean

  def send(user: User, email: EmailAddress)(implicit lang: Lang): Funit

  def confirm(token: String): Fu[EmailConfirm.Result]
}

object EmailConfirmSkip extends EmailConfirm {

  def effective = false

  def send(user: User, email: EmailAddress)(implicit lang: Lang) = UserRepo setEmailConfirmed user.id

  def confirm(token: String): Fu[EmailConfirm.Result] = fuccess(EmailConfirm.Result.NotFound)
}

final class EmailConfirmMailgun(
    mailgun: Mailgun,
    baseUrl: String,
    tokenerSecret: String
) extends EmailConfirm {

  import Mailgun.html._

  def effective = true

  val maxTries = 3

  def send(user: User, email: EmailAddress)(implicit lang: Lang): Funit = tokener make user.id flatMap { token =>
    lila.mon.email.types.confirmation()
    val url = s"$baseUrl/signup/confirm/$token"
    lila.log("auth").info(s"Confirm URL ${user.username} ${email.value} $url")
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
      htmlBody = emailMessage(
        pDesc(trans.emailConfirm_click.literalTo(lang)),
        potentialAction(metaName("Activate account"), Mailgun.html.url(url)),
        publisher(
          small(
            trans.common_note.literalTo(lang, List(Mailgun.html.noteLink)),
            " ",
            trans.emailConfirm_ignore.literalTo(lang)
          )
        )
      ).some
    )
  }

  import EmailConfirm.Result

  def confirm(token: String): Fu[Result] = tokener read token flatMap {
    _ ?? UserRepo.enabledById
  } flatMap {
    _.fold[Fu[Result]](fuccess(Result.NotFound)) { user =>
      UserRepo.mustConfirmEmail(user.id) flatMap {
        case true => (UserRepo setEmailConfirmed user.id) inject Result.JustConfirmed(user)
        case false => fuccess(Result.AlreadyConfirmed(user))
      }
    }
  }

  private val tokener = new StringToken[User.ID](
    secret = tokenerSecret,
    getCurrentValue = id => UserRepo email id map (_.??(_.value))
  )
}

object EmailConfirm {

  sealed trait Result
  object Result {
    case class JustConfirmed(user: User) extends Result
    case class AlreadyConfirmed(user: User) extends Result
    case object NotFound extends Result
  }

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

  object Help {

    sealed trait Status { val name: String }
    case class NoSuchUser(name: String) extends Status
    case class Closed(name: String) extends Status
    case class Confirmed(name: String) extends Status
    case class NoEmail(name: String) extends Status
    case class EmailSent(name: String, email: EmailAddress) extends Status

    import play.api.data._
    import play.api.data.validation.Constraints
    import play.api.data.Forms._

    val helpForm = Form(
      single("username" -> text.verifying(
        Constraints minLength 2,
        Constraints maxLength 30,
        Constraints.pattern(regex = User.newUsernameRegex)
      ))
    )

    def getStatus(username: String): Fu[Status] = UserRepo withEmails username flatMap {
      case None => fuccess(NoSuchUser(username))
      case Some(User.WithEmails(user, emails)) =>
        if (!user.enabled) fuccess(Closed(username))
        else UserRepo mustConfirmEmail user.id map {
          case true => emails.current match {
            case None => NoEmail(user.username)
            case Some(email) => EmailSent(user.username, email)
          }
          case false => Confirmed(user.username)
        }
    }
  }
}
