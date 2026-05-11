package lila.security

import play.api.i18n.Lang
import play.api.mvc.{ Cookie, Session, RequestHeader }
import scalatags.Text.all.*

import lila.core.config.*
import lila.core.i18n.Translate
import lila.core.i18n.I18nKey.emails as trans
import lila.core.net.ValidReferrer
import lila.mailer.Mailer
import lila.user.{ User, UserApi, UserRepo }

trait EmailConfirm:

  def effective: Boolean

  def send(user: User, email: EmailAddress)(using Lang, Option[ValidReferrer]): Funit

  def dryTest(token: String): Fu[EmailConfirm.Result]

  def confirm(token: String): Fu[EmailConfirm.Result]

final class EmailConfirmSkip(userRepo: UserRepo) extends EmailConfirm:

  def effective = false

  def send(user: User, email: EmailAddress)(using Lang, Option[ValidReferrer]) =
    userRepo.setEmailConfirmed(user.id).void

  def dryTest(token: String): Fu[EmailConfirm.Result] = fuccess(EmailConfirm.Result.NotFound)

  def confirm(token: String): Fu[EmailConfirm.Result] = fuccess(EmailConfirm.Result.NotFound)

final class EmailConfirmMailer(
    userRepo: UserRepo,
    mailer: Mailer,
    routeUrl: RouteUrl,
    tokenerSecret: Secret
)(using Executor, lila.core.i18n.Translator)
    extends EmailConfirm:

  import Mailer.html.*

  def effective = true

  val maxTries = 3

  def send(user: User, email: EmailAddress)(using lang: Lang, referrer: Option[ValidReferrer]): Funit =
    if email.looksLikeFakeEmail then
      lila.log("auth").info(s"Not sending confirmation to fake email $email of ${user.username}")
      fuccess(())
    else
      email.looksLikeFakeEmail.not.so:
        tokener.make(user.id).flatMap { token =>
          lila.mon.email.send.confirmation.increment()
          val url = referrer.foldLeft(routeUrl(routes.Auth.signupConfirmEmail(token))): (url, ref) =>
            ref.propagate(url)
          lila.log("auth").info(s"Confirm URL ${user.username} ${email.value} $url")
          mailer.sendOrFail:
            Mailer.Message(
              to = email,
              subject = trans.emailConfirm_subject.txt(user.username),
              text = Mailer.txt.addServiceNote(EmailConfirm.emailText(url)),
              htmlBody = emailMessage(
                pDesc(trans.emailConfirm_intro()),
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

final class EmailConfirmByUserSend(
    securityForm: SecurityForm,
    emailValidator: EmailAddressValidator,
    userRepo: UserRepo,
    mailer: lila.mailer.AutomaticEmail
)(using Executor, lila.core.config.RateLimit):

  case class Data(sender: EmailAddress, to: EmailAddress):
    def userAndMillis: Option[(UserStr, Int)] =
      to.username.split('.') match
        case Array(u, m) => for user <- UserStr.read(u); millis <- m.toIntOption yield user -> millis
        case _ => none

  import play.api.data.*
  import play.api.data.Forms.*
  import lila.memo.RateLimit

  def workerForm(using Me) = Form:
    mapping(
      "sender" -> securityForm.sendableEmail, // player.email@example.com
      "to" -> securityForm.anyEmail // username.millis@verify.lichess.org
    )(Data.apply)(unapply)

  enum Result:
    case invalid, notFound, rateLimit, milliMismatch, alreadyConfirmed, emailInUse
    case confirm(user: User, email: EmailAddress) extends Result

  def process(data: Data)(using Me): Fu[Option[(User, EmailAddress)]] =
    resultOf(data)
      .addEffect: res =>
        logger.branch("emailConfirmByUser").info(s"$res $data")
        val resKey = res match
          case Result.confirm(_, _) => "success"
          case r => r.toString
        lila.mon.user.register
          .modConfirmEmail(by = "worker", result = resKey)
          .increment()
      .flatMap:
        case Result.confirm(user, email) =>
          given Lang = user.realLang | lila.core.i18n.defaultLang
          for
            _ <- mailer.welcomeEmail(user, email)
            _ <- mailer.welcomePM(user)
          yield Some(user -> email)
        case Result.emailInUse =>
          for _ <- mailer.emailAlreadyInUse(data.sender)
          yield none
        case Result.alreadyConfirmed =>
          for _ <- mailer.alreadyConfirmed(data.sender)
          yield none
        case _ => fuccess(none) // don't send an email

  private def resultOf(d: Data): Fu[Result] =
    d.userAndMillis.fold(fuccess(Result.invalid)): (userId, millis) =>
      userRepo
        .enabledById(userId)
        .flatMap:
          _.fold(fuccess(Result.notFound)): user =>
            rateLimitPerUser(user.id, fuccess(Result.rateLimit)):
              rateLimitPerEmail(d.sender, fuccess(Result.rateLimit)):
                if EmailConfirm.creationMillis(user) != millis then fuccess(Result.milliMismatch)
                else if user.everLoggedIn then fuccess(Result.alreadyConfirmed)
                else
                  for ok <- emailValidator.uniqueAsync(d.sender, user.some)
                  yield if ok then Result.confirm(user, d.sender) else Result.emailInUse

  private lazy val rateLimitPerUser = RateLimit[UserId](
    credits = 4,
    duration = 1.hour,
    key = "user.email.confirm.user"
  )
  private lazy val rateLimitPerEmail = RateLimit[EmailAddress](
    credits = 4,
    duration = 1.hour,
    key = "user.email.confirm.email"
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

    def newSession(lilaCookie: LilaCookie, user: User, email: EmailAddress)(using RequestHeader): Cookie =
      lilaCookie.withSession(remember = false): _ =>
        Session.emptyCookie + (name -> s"${user.username}$sep${email.value}")

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

  def creationMillis(user: User) =
    user.createdAt.atZone(java.time.ZoneOffset.UTC).getNano / 1_000_000

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
      case EmailSent(name: UserName, email: EmailAddress, sendTo: EmailAddress)

    import play.api.data.*
    import play.api.data.Forms.*

    val helpForm = Form:
      single("username" -> lila.common.Form.username.historicalField)

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
                      case Some(email) =>
                        val sendTo = EmailAddress:
                          s"${user.username}.${EmailConfirm.creationMillis(user)}@verify.lichess.org"
                        EmailSent(user.username, email, sendTo)
                  else Confirmed(user.username)

  private[security] def emailText(url: Url)(using Translate) = s"""
${trans.emailConfirm_intro.txt()}

${trans.emailConfirm_click.txt()}

$url

${trans.common_orPaste.txt()}

${trans.emailConfirm_justIgnore.txt("https://lichess.org")}
"""
