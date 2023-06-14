package lila.security

import play.api.i18n.Lang
import play.api.mvc.{ Cookie, RequestHeader }
import scalatags.Text.all.*

import lila.common.config.*
import lila.common.{ EmailAddress, LilaCookie }
import lila.i18n.I18nKeys.{ emails as trans }
import lila.user.{ User, UserRepo }
import lila.mailer.Mailer

trait EmailConfirm:

  def effective: Boolean

  def send(user: User, email: EmailAddress)(using Lang): Funit

  def confirm(token: String): Fu[EmailConfirm.Result]

final class EmailConfirmSkip(userRepo: UserRepo) extends EmailConfirm:

  def effective = false

  def send(user: User, email: EmailAddress)(using Lang) =
    userRepo setEmailConfirmed user.id void

  def confirm(token: String): Fu[EmailConfirm.Result] = fuccess(EmailConfirm.Result.NotFound)

final class EmailConfirmMailer(
    userRepo: UserRepo,
    mailer: Mailer,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
)(using Executor)
    extends EmailConfirm:

  import Mailer.html.*

  def effective = true

  val maxTries = 3

  def send(user: User, email: EmailAddress)(using Lang): Funit =
    !email.looksLikeFakeEmail so {
      tokener make user.id flatMap { token =>
        lila.mon.email.send.confirmation.increment()
        val url = s"$baseUrl/signup/confirm/$token"
        lila.log("auth").info(s"Confirm URL ${user.username} ${email.value} $url")
        mailer send Mailer.Message(
          to = email,
          subject = trans.emailConfirm_subject.txt(user.username),
          text = Mailer.txt.addServiceNote(s"""
${trans.emailConfirm_click.txt()}

$url

${trans.common_orPaste.txt()}

${trans.emailConfirm_ignore.txt("https://lichess.org")}
"""),
          htmlBody = emailMessage(
            pDesc(trans.emailConfirm_click()),
            potentialAction(metaName("Activate account"), Mailer.html.url(url)),
            small(trans.emailConfirm_ignore()),
            serviceNote
          ).some
        )
      }
    }

  import EmailConfirm.Result

  def confirm(token: String): Fu[Result] =
    tokener read token flatMapz userRepo.enabledById flatMap {
      _.fold[Fu[Result]](fuccess(Result.NotFound)) { user =>
        userRepo.mustConfirmEmail(user.id) flatMap {
          if _ then (userRepo setEmailConfirmed user.id) inject Result.JustConfirmed(user)
          else fuccess(Result.AlreadyConfirmed(user))
        }
      }
    }

  private val tokener = new StringToken[UserId](
    secret = tokenerSecret,
    getCurrentValue = id => userRepo email id dmap (_.so(_.value))
  )

object EmailConfirm:

  enum Result:
    case JustConfirmed(user: User)
    case AlreadyConfirmed(user: User)
    case NotFound

  case class UserEmail(username: UserName, email: EmailAddress)

  object cookie:

    val name        = "email_confirm"
    private val sep = ":"

    def make(lilaCookie: LilaCookie, user: User, email: EmailAddress)(using RequestHeader): Cookie =
      lilaCookie.session(
        name = name,
        value = s"${user.username}$sep${email.value}"
      )

    def has(req: RequestHeader) = req.session.data contains name

    def get(req: RequestHeader): Option[UserEmail] =
      req.session get name map (_.split(sep, 2)) collect { case Array(username, email) =>
        UserEmail(UserName(username), EmailAddress(email))
      }

  import play.api.mvc.RequestHeader
  import lila.memo.RateLimit
  import lila.common.{ HTTPRequest, IpAddress }

  private lazy val rateLimitPerIP = RateLimit[IpAddress](
    credits = 40,
    duration = 1 hour,
    key = "email.confirms.ip"
  )

  private lazy val rateLimitPerUser = RateLimit[UserId](
    credits = 3,
    duration = 1 hour,
    key = "email.confirms.user"
  )

  private lazy val rateLimitPerEmail = RateLimit[String](
    credits = 3,
    duration = 1 hour,
    key = "email.confirms.email"
  )

  def rateLimit[A](userEmail: UserEmail, req: RequestHeader, default: => Fu[A])(run: => Fu[A]): Fu[A] =
    rateLimitPerUser(userEmail.username.id, default):
      rateLimitPerEmail(userEmail.email.value, default):
        rateLimitPerIP(HTTPRequest ipAddress req, default):
          run

  object Help:

    enum Status:
      val name: UserName
      case NoSuchUser(name: UserName)
      case Closed(name: UserName)
      case Confirmed(name: UserName)
      case NoEmail(name: UserName)
      case EmailSent(name: UserName, email: EmailAddress)

    import play.api.data.*
    import play.api.data.Forms.*

    val helpForm = Form(
      single("username" -> lila.user.UserForm.historicalUsernameField)
    )

    def getStatus(userRepo: UserRepo, u: UserStr)(using Executor): Fu[Status] =
      import Status.*
      userRepo withEmails u flatMap {
        case None => fuccess(NoSuchUser(u into UserName))
        case Some(User.WithEmails(user, emails)) =>
          if (user.enabled.no) fuccess(Closed(user.username))
          else
            userRepo mustConfirmEmail user.id dmap {
              if _ then
                emails.current match
                  case None        => NoEmail(user.username)
                  case Some(email) => EmailSent(user.username, email)
              else Confirmed(user.username)
            }
      }
