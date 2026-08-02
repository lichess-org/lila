package lila.security

import play.api.mvc.RequestHeader
import play.api.i18n.Lang
import scalalib.net.{ Bearer, UserAgent }

import lila.core.config.*
import lila.core.i18n.I18nKey.emails as trans
import lila.core.net.{ Origin, ValidReferrer }
import lila.core.email.NormalizedEmailAddress
import lila.mailer.Mailer
import lila.user.{ User, UserRepo }
import lila.oauth.{ AccessTokenApi, OAuthScope, TokenScopes }
import lila.common.HTTPRequest
import lila.memo.RateLimit.LimitResult

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

    private val store =
      cacheApi.notLoadingSync[(NormalizedEmailAddress, Code), UserId](64, "loginToken.storedCode"):
        _.expireAfterWrite(5.minutes).build()

    private val chars = (('2' to '9') ++ (('a' to 'z').toSet - 'l')).mkString
    private val nbChars = chars.length
    private def secureChar = chars(scalalib.SecureRandom.nextInt(nbChars))

    private def reqEmail(using RequestHeader): Option[NormalizedEmailAddress] =
      HTTPRequest.queryStringGet("email").flatMap(EmailAddress.from).map(_.normalize)

    def consume()(using RequestHeader, UserAgent): Fu[LimitResult | Bearer] =
      (reqEmail, HTTPRequest.queryStringGet("code")).tupled.fold(notRateLimited): pair =>
        limitAndFind(pair._1, cost = 1): (user, _) =>
          if store.getIfPresent(pair).exists(_.is(user))
          then
            store.invalidate(pair)
            val scopes = TokenScopes(List(OAuthScope.Web.Mobile))
            accessTokenApi.create(user.id, scopes, Origin("org.lichess.mobile://")).map(_.plain)
          else notRateLimited

    def createAndSend()(using RequestHeader): Fu[LimitResult] =
      reqEmail.fold(notRateLimited): rawEmail =>
        limitAndFind(rawEmail, cost = 1): (user, email) =>
          val code = String(Array.fill(6)(secureChar))
          store.put(rawEmail -> code, user.id)
          lila.mon.email.send.storedCode.increment()
          import scalatags.Text.all.*
          import Mailer.html.*
          sendEmail(user, email)(
            List(
              "Enter this code to log in with your Lichess account:",
              "",
              code,
              "",
              "This code expires in 5 minutes. If you didn’t request it, you can safely ignore this email."
            ),
            emailMessage(
              p("Enter this code to log in with your Lichess account:"),
              loginCode(metaName("Log in code"), code),
              p(
                "This code expires in 5 minutes. If you didn’t request it, you can safely ignore this email."
              ),
              serviceNote
            )
          ).inject(LimitResult.Through)

  object magicLink:

    private val tokener = StringToken.withLifetime[UserId](tokenerSecret, 10.minutes)

    def generate[U: UserIdOf](user: U): Fu[String] = tokener.make(user.id)

    def send(
        reqEmail: EmailAddress
    )(using req: RequestHeader, referrer: Option[ValidReferrer]): Fu[LimitResult] =
      limitAndFind(reqEmail.normalize, cost = 2): (user, email) =>
        generate(user).flatMap { token =>
          lila.mon.email.send.magicLink.increment()
          val url = referrer.foldLeft(routeUrl(routes.Auth.loginWithToken(token))): (url, ref) =>
            ref.propagate(url)
          import scalatags.Text.all.*
          import Mailer.html.*
          sendEmail(user, email)(
            List(trans.passwordReset_clickOrIgnore.txt(), "", url.value, "", trans.common_orPaste.txt()),
            emailMessage(
              p(trans.passwordReset_clickOrIgnore()),
              potentialAction(metaName("Log in"), Mailer.html.url(url)),
              serviceNote
            )
          ).inject(LimitResult.Through)
        }

    def consume(token: String): Fu[Option[User]] =
      tokener.read(token).flatMapz(userRepo.notForeverClosedById)

  private def limitAndFind[A](email: NormalizedEmailAddress, cost: Int)(f: (User, EmailAddress) => Fu[A])(
      using req: RequestHeader
  ): Fu[LimitResult | A] =
    rateLimit(email, fuccess(LimitResult.Limited), cost):
      userRepo
        .notClosedForeverWithEmail(email)
        .flatMap(_.fold(notRateLimited)(f.tupled))

  private val notRateLimited = fuccess(LimitResult.Through) // but we don't tell what was wrong

  private def sendEmail(user: User, email: EmailAddress)(
      makeText: Lang ?=> List[String],
      makeHtml: Lang ?=> scalatags.Text.all.Frag
  ): Funit =
    given play.api.i18n.Lang = user.realLang | lila.core.i18n.defaultLang
    mailer.sendOrFail:
      Mailer.Message(
        to = email,
        subject = trans.logInToLichess.txt(user.username),
        text = Mailer.txt.addServiceNote(makeText.mkString("\n")),
        htmlBody = makeHtml.some
      )

  object rateLimit:

    import play.api.mvc.RequestHeader
    import lila.memo.RateLimit
    import lila.common.HTTPRequest
    import lila.core.net.IpAddress

    private val defaultCost = 2

    private lazy val rateLimitPerIP = RateLimit[IpAddress](
      credits = 10 * defaultCost,
      duration = 1.hour,
      key = "login.magicLink.ip"
    )

    private lazy val rateLimitPerEmail = RateLimit[String](
      credits = 3 * defaultCost,
      duration = 1.hour,
      key = "login.magicLink.email"
    )

    def apply[A](email: NormalizedEmailAddress, default: => Fu[A], cost: Int = defaultCost)(run: => Fu[A])(
        using req: RequestHeader
    ): Fu[A] =
      rateLimitPerEmail(email.value, default, cost):
        rateLimitPerIP(HTTPRequest.ipAddress(req), default, cost):
          run
