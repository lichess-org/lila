package lila.security

import play.api.i18n.Lang
import play.api.mvc.{ Cookie, RequestHeader }
import scalatags.Text.all.*

import lila.core.config.*
import lila.core.i18n.I18nKey.emails as trans
import lila.mailer.Mailer
import lila.user.{ User, UserApi, UserRepo }

trait EmailConfirm:

  def effective: Boolean

  def send(user: User, email: EmailAddress)(using Lang): Funit

  def dryTest(token: String): Fu[EmailConfirm.Result]

  def confirm(token: String): Fu[EmailConfirm.Result]

final class EmailConfirmSkip(userRepo: UserRepo) extends EmailConfirm:

  def effective = false

  def send(user: User, email: EmailAddress)(using Lang) =
    userRepo.setEmailConfirmed(user.id).void

  def dryTest(token: String): Fu[EmailConfirm.Result] = fuccess(EmailConfirm.Result.NotFound)

  def confirm(token: String): Fu[EmailConfirm.Result] = fuccess(EmailConfirm.Result.NotFound)

final class EmailConfirmMailer(
    userRepo: UserRepo,
    mailer: Mailer,
    baseUrl: BaseUrl,
    tokenerSecret: Secret
)(using Executor, lila.core.i18n.Translator)
    extends EmailConfirm:

  import Mailer.html.*

  def effective = true

  val maxTries = 3

  def send(user: User, email: EmailAddress)(using Lang): Funit =
    if email.looksLikeFakeEmail then
      lila.log("auth").info(s"Not sending confirmation to fake email $email of ${user.username}")
      fuccess(())
    else
      email.looksLikeFakeEmail.not.so:
        tokener.make(user.id).flatMap { token =>
          lila.mon.email.send.confirmation.increment()
          val url = s"$baseUrl/signup/confirm/$token"
          lila.log("auth").info(s"Confirm URL ${user.username} ${email.value} $url")
          mailer.sendOrFail:
            Mailer.Message(
              to = email,
              subject = trans.emailConfirm_subject.txt(user.username),
              text = Mailer.txt.addServiceNote(s"""
${trans.emailConfirm_click.txt()}

$url

${trans.common_orPaste.txt()}

${trans.emailConfirm_justIgnore.txt("https://lichess.org")}
"""),
              htmlBody = emailMessage(
                pDesc(trans.emailConfirm_click()),
                potentialAction(metaName("Activate account"), Mailer.html.url(url)),
                small(trans.emailConfirm_justIgnore()),
                serviceNote
              ).some
            )
        }

  import EmailConfirm.Result.*

  def dryTest(token: String): Fu[EmailConfirm.Result] =
    tokener
      .read(token)
      .flatMapz(userRepo.enabledById)
      .flatMap:
        _.fold(fuccess(NotFound)): user =>
          userRepo
            .mustConfirmEmail(user.id)
            .map:
              if _ then NeedsConfirm(user) else AlreadyConfirmed(user)

  def confirm(token: String): Fu[EmailConfirm.Result] =
    dryTest(token).flatMap:
      case NeedsConfirm(user) => userRepo.setEmailConfirmed(user.id).inject(JustConfirmed(user))
      case other => fuccess(other)

  private val tokener = StringToken[UserId](
    secret = tokenerSecret,
    getCurrentValue = id => userRepo.email(id).dmap(_.so(_.value))
  )

object EmailConfirm:

  enum Result:
    case NeedsConfirm(user: User)
    case JustConfirmed(user: User)
    case AlreadyConfirmed(user: User)
    case NotFound

  case class UserEmail(username: UserName, email: EmailAddress)

  object cookie:

    val name = "email_confirm"
    private val sep = ":"

    def make(lilaCookie: LilaCookie, user: User, email: EmailAddress)(using RequestHeader): Cookie =
      lilaCookie.session(
        name = name,
        value = s"${user.username}$sep${email.value}"
      )

    def has(req: RequestHeader) = req.session.data contains name

    def get(req: RequestHeader): Option[UserEmail] =
      req.session.get(name).map(_.split(sep, 2)).collect { case Array(username, email) =>
        UserEmail(UserName(username), EmailAddress(email))
      }

  import lila.memo.RateLimit
  import lila.common.HTTPRequest
  import lila.core.net.IpAddress
  given Executor = scala.concurrent.ExecutionContextOpportunistic
  given lila.core.config.RateLimit = lila.core.config.RateLimit.Yes

  private lazy val rateLimitPerIP = RateLimit[IpAddress](
    credits = 40,
    duration = 1.hour,
    key = "email.confirms.ip"
  )

  private lazy val rateLimitPerUser = RateLimit[UserId](
    credits = 3,
    duration = 1.hour,
    key = "email.confirms.user"
  )

  private lazy val rateLimitPerEmail = RateLimit[String](
    credits = 3,
    duration = 1.hour,
    key = "email.confirms.email"
  )

  def rateLimit[A](userEmail: UserEmail, req: RequestHeader, default: => Fu[A])(run: => Fu[A]): Fu[A] =
    rateLimitPerUser(userEmail.username.id, default):
      rateLimitPerEmail(userEmail.email.value, default):
        rateLimitPerIP(HTTPRequest.ipAddress(req), default):
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
      single("username" -> lila.common.Form.username.historicalField)
    )

    def getStatus(userApi: UserApi, userRepo: UserRepo, u: UserStr)(using Executor): Fu[Status] =
      import Status.*
      userApi
        .withEmails(u)
        .flatMap:
          case None => fuccess(NoSuchUser(u.into(UserName)))
          case Some(lila.core.user.WithEmails(user, emails)) =>
            if user.enabled.no then fuccess(Closed(user.username))
            else
              userRepo
                .mustConfirmEmail(user.id)
                .dmap:
                  if _ then
                    emails.current match
                      case None => NoEmail(user.username)
                      case Some(email) => EmailSent(user.username, email)
                  else Confirmed(user.username)
