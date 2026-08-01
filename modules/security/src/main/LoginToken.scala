package lila.security

import play.api.mvc.RequestHeader
import play.api.i18n.Lang
import scalalib.net.{ Bearer, UserAgent }

import lila.core.config.*
import lila.core.i18n.I18nKey.emails as trans
import lila.core.net.{ Origin, ValidReferrer }
import lila.mailer.Mailer
import lila.user.{ User, UserRepo }
import lila.oauth.{ AccessTokenApi, OAuthScope, TokenScopes }
import lila.common.HTTPRequest

final class LoginToken(
    mailer: Mailer,
    userRepo: UserRepo,
    routeUrl: RouteUrl,
    tokenerSecret: Secret,
    cacheApi: lila.memo.CacheApi,
    accessTokenApi: AccessTokenApi
)(using Executor, lila.core.i18n.Translator, lila.core.config.RateLimit):

  object storedCode:

    private type Code = String

    private val store = cacheApi.notLoadingSync[(EmailAddress, Code), UserId](64, "loginToken.storedCode"):
      _.expireAfterWrite(5.minutes).build()

    private val chars = (('2' to '9') ++ (('a' to 'z').toSet - 'l')).mkString
    private val nbChars = chars.length
    private def secureChar = chars(scalalib.SecureRandom.nextInt(nbChars))

    private def reqEmail(using req: RequestHeader): Option[EmailAddress] =
      HTTPRequest.queryStringGet("email").flatMap(EmailAddress.from)

    def consume()(using RequestHeader, UserAgent): Fu[Option[Bearer]] =
      (reqEmail, HTTPRequest.queryStringGet("code")).tupled.so: pair =>
        findAndLimit(rawEmail, cost = 1): (user, email) =>
          store
            .getIfPresent(pair)
            .so: userId =>
              store.invalidate(pair)
              userRepo
                .notForeverClosedById(userId)
                .flatMapz: user =>
                  val scopes = TokenScopes(List(OAuthScope.Web.Mobile))
                  accessTokenApi
                    .create(user.id, scopes, Origin("org.lichess.mobile://"))
                    .map: token =>
                      token.plain.some

    def createAndSend()(using req: RequestHeader): Fu[Boolean] =
      reqEmail.so: rawEmail =>
        findAndLimit(rawEmail, cost = 1): (user, email) =>
          val code = String(Array.fill(6)(secureChar))
          store.put(rawEmail -> code, user.id)
          lila.mon.email.send.storedCode.increment()
          import scalatags.Text.all.*
          import Mailer.html.*
          sendEmail(user, email)(
            Mailer.txt.addServiceNote(s"""
Enter this code to log in with your Lichess account:

$code

This code expires in 5 minutes. If you didn’t request it, you can safely ignore this email.
"""),
            emailMessage(
              p("Enter this code to log in with your Lichess account:"),
              loginCode(metaName("Log in code"), code),
              p(
                "This code expires in 5 minutes. If you didn’t request it, you can safely ignore this email."
              ),
              serviceNote
            )
          )

  object magicLink:

    private val tokener = StringToken.withLifetime[UserId](tokenerSecret, 10.minutes)

    def generate[U: UserIdOf](user: U): Fu[String] = tokener.make(user.id)

    def send(reqEmail: EmailAddress)(using req: RequestHeader, referrer: Option[ValidReferrer]): Fu[Boolean] =
      findAndLimit(reqEmail, cost = 2): (user, email) =>
        generate(user).flatMap { token =>
          lila.mon.email.send.magicLink.increment()
          val url = referrer.foldLeft(routeUrl(routes.Auth.loginWithToken(token))): (url, ref) =>
            ref.propagate(url)
          import scalatags.Text.all.*
          import Mailer.html.*
          sendEmail(user, email)(
            Mailer.txt.addServiceNote(s"""
${trans.passwordReset_clickOrIgnore.txt()}

$url

${trans.common_orPaste.txt()}"""),
            emailMessage(
              p(trans.passwordReset_clickOrIgnore()),
              potentialAction(metaName("Log in"), Mailer.html.url(url)),
              serviceNote
            )
          )
        }

    def consume(token: String): Fu[Option[User]] =
      tokener.read(token).flatMapz(userRepo.notForeverClosedById)

  private def findAndLimit(reqEmail: EmailAddress, cost: Int)(f: (User, EmailAddress) => Funit)(using
      req: RequestHeader
  ): Fu[Boolean] =
    userRepo
      .notClosedForeverWithEmail(reqEmail.normalize)
      .flatMapz: (user, email) =>
        rateLimit(user, email, cost = cost, fuFalse)(f(user, email).inject(true))

  private def sendEmail(user: User, email: EmailAddress)(
      makeText: Lang ?=> String,
      makeHtml: Lang ?=> scalatags.Text.all.Frag
  ): Funit =
    given play.api.i18n.Lang = user.realLang | lila.core.i18n.defaultLang
    mailer.sendOrFail:
      Mailer.Message(
        to = email,
        subject = trans.logInToLichess.txt(user.username),
        text = makeText,
        htmlBody = makeHtml.some
      )

  object rateLimit:

    import play.api.mvc.RequestHeader
    import lila.memo.RateLimit
    import lila.common.HTTPRequest
    import lila.core.net.IpAddress

    private lazy val rateLimitPerIP = RateLimit[IpAddress](
      credits = 10 * 2,
      duration = 1.hour,
      key = "login.magicLink.ip"
    )

    private lazy val rateLimitPerUser = RateLimit[UserId](
      credits = 3 * 2,
      duration = 1.hour,
      key = "login.magicLink.user"
    )

    private lazy val rateLimitPerEmail = RateLimit[String](
      credits = 3 * 2,
      duration = 1.hour,
      key = "login.magicLink.email"
    )

    def apply[A](user: User, email: EmailAddress, cost: Int, default: => Fu[A])(
        run: => Fu[A]
    )(using req: RequestHeader): Fu[A] =
      rateLimitPerUser(user.id, default):
        rateLimitPerEmail(email.value, default):
          rateLimitPerIP(HTTPRequest.ipAddress(req), default):
            run
