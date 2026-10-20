package controllers
import play.api.data.{ Form, FormError }
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.email.{ UserIdOrEmail, UserStrOrEmail }
import lila.core.net.IpAddress
import lila.core.security.ClearPassword
import lila.memo.RateLimit
import lila.oidc.{ LoginError, LoginOutcome }
import lila.security.SecurityForm.{ MagicLink, PasswordReset }
import lila.security.{ FingerPrint, Signup }
import lila.security.EmailConfirm

final class Auth(
    env: Env,
    accountC: => Account
) extends LilaController(env):

  import env.security.{ api, forms }

  private def mobileUserOk(u: UserModel, sessionId: String)(using Context): Fu[Result] = for
    povs  <- env.round.proxyRepo.urgentGames(u)
    perfs <- ctx.pref.showRatings.soFu(env.user.perfsRepo.perfsOf(u))
  yield Ok:
    env.user.jsonView.full(u, perfs, withProfile = true) ++ Json.obj(
      "nowPlaying" -> JsArray(povs.take(20).map(env.api.lobbyApi.nowPlaying)),
      "sessionId"  -> sessionId
    )

  private def getReferrerOption(using ctx: Context): Option[String] =
    get("referrer").flatMap(env.web.referrerRedirect.valid).orElse(ctx.req.session.get(api.AccessUri))

  private def getReferrer(using Context): String = getReferrerOption | routes.Lobby.home.url

  def authenticateUser(
      u: UserModel,
      remember: Boolean,
      result: Option[String => Result] = None,
      oidcSession: Option[lila.oidc.OidcSession] = None
  )(using ctx: Context): Fu[Result] =
    api
      .saveAuthentication(
        u.id,
        ctx.mobileApiVersion,
        checkEmailConfirmation = !env.ssoOnly,
        oidcSession = oidcSession
      )
      .flatMap: sessionId =>
        negotiate(
          result.fold(Redirect(getReferrer))(_(getReferrer)),
          mobileUserOk(u, sessionId)
        ).map(authenticateCookie(sessionId, remember))
      .recoverWith(authRecovery)

  private def authenticateAppealUser(u: UserModel, redirect: String => Result, url: String)(using
      ctx: Context
  ): Fu[Result] =
    api.appeal
      .saveAuthentication(u.id)
      .flatMap: sessionId =>
        authenticateCookie(sessionId, remember = false):
          redirect(url)
      .recoverWith(authRecovery)

  private def authenticateCookie(sessionId: String, remember: Boolean)(
      result: Result
  )(using RequestHeader) =
    result.withCookies(
      env.security.lilaCookie.withSession(remember = remember) {
        _ + (api.sessionIdKey -> sessionId) - api.AccessUri - EmailConfirm.cookie.name
      }
    )

  /** Deny local authentication flows when the app runs in SSO-only mode. */
  private def localOpen(f: Context ?=> Fu[Result]): EssentialAction =
    Open:
      if env.ssoOnly then notFound
      else f(using summon[Context])

  private def localOpenBody(f: BodyContext[?] ?=> Fu[Result]): EssentialAction =
    OpenBody:
      if env.ssoOnly then notFound
      else f(using summon[BodyContext[?]])

  private def authRecovery(using ctx: Context): PartialFunction[Throwable, Fu[Result]] =
    case lila.security.SecurityApi.MustConfirmEmail(_) =>
      if HTTPRequest.isXhr(ctx.req)
      then Ok(s"ok:${routes.Auth.checkYourEmail}")
      else BadRequest.async(accountC.renderCheckYourEmail)

  def login     = Open(oidcLogin)
  def loginLang = LangPage(routes.Auth.login)(oidcLogin)

  private def serveLogin(using ctx: Context) = NoBot:
    val referrer = get("referrer").flatMap(env.web.referrerRedirect.valid)
    val switch   = get("switch").orElse(get("as"))
    referrer.ifTrue(ctx.isAuth).ifTrue(switch.isEmpty) match
      case Some(url) => Redirect(url) // redirect immediately if already logged in
      case None =>
        val prefillUsername = UserStrOrEmail(~switch.filter(_ != "1"))
        val form            = api.loginFormFilled(prefillUsername)
        Ok.page(views.auth.login(form, referrer)).map(_.withCanonical(routes.Auth.login))

  private def oidcLogin(using ctx: Context) = NoBot:
    val referrer = get("referrer").flatMap(env.web.referrerRedirect.valid)
    val switch   = get("switch").orElse(get("as"))
    referrer.ifTrue(ctx.isAuth).ifTrue(switch.isEmpty) match
      case Some(url) => Redirect(url) // redirect immediately if already logged in
      case None =>
        env.oidc.api.getAuthenticationRequest().flatMap { (request, codeVerifier) =>
          env.oidc.api.saveLoginState(request, referrer, codeVerifier).map { _ =>
            Redirect(request.toURI.toString)
              .flashing("referrer" -> referrer.getOrElse("/"))
              .withCookies(
                env.security.lilaCookie.cookie(
                  "oidc_state",
                  request.getState.getValue,
                  maxAge = Some(600),
                  httpOnly = Some(true)
                )
              )
          }
        }

  def signinOidc = Open(oidcCallback)

  private def oidcCallback(using ctx: Context) = NoBot:
    env.oidc.api.authenticate(ctx.req).flatMap {
      case LoginOutcome.Success(user, oidcSession) =>
        authenticateUser(user, remember = true, oidcSession = oidcSession)
      case LoginOutcome.Refused(LoginError.Closed, Some(user)) =>
        env.mod.logApi.closedByMod(user).flatMap { closedByMod =>
          if closedByMod then oidcRefusalPage(LoginError.Closed)
          else
            env.mod.logApi.closedByTeacher(user).flatMap { closedByTeacher =>
              if closedByTeacher then oidcRefusalPage(LoginError.Closed)
              else authenticateAppealUser(user, url => Redirect(url), routes.Account.reopen.url)
            }
        }
      case LoginOutcome.Refused(
            error @ (LoginError.Banned | LoginError.Deleted | LoginError.ClosedForever | LoginError.Closed),
            _
          ) =>
        oidcRefusalPage(error)
      case LoginOutcome.Refused(_, _) =>
        fuccess(Redirect(routes.Auth.login).flashFailure)
    }.map(_.discardingCookies(env.security.lilaCookie.discard("oidc_state")))

  private def oidcRefusalPage(refusal: LoginError)(using Context) =
    import scalatags.Text.all.stringFrag
    refusal match
      case LoginError.Deleted =>
        Forbidden.page(views.site.message("Account unavailable")(stringFrag("This account is no longer available.")))
      case LoginError.ClosedForever =>
        Forbidden.page(views.site.message("Account closed")(stringFrag("This account has been permanently closed.")))
      case LoginError.Banned =>
        Forbidden.page(views.site.message("Account closed")(stringFrag("This account is closed.")))
      case LoginError.Closed =>
        Forbidden.page(views.site.message("Account closed")(stringFrag("This account is closed.")))
      case _ =>
        Forbidden.page(views.site.message("Authentication error")(stringFrag("Please try again.")))

  private val is2fa = Set("MissingTotpToken", "InvalidTotpToken")

  def authenticate = localOpenBody:
    NoCrawlers:
      Firewall:
        def redirectTo(url: String) = if HTTPRequest.isXhr(ctx.req) then Ok(s"ok:$url") else Redirect(url)
        val referrer                = get("referrer").filterNot(env.web.referrerRedirect.sillyLoginReferrers)
        val isRemember              = api.rememberForm.bindFromRequest().value | true
        bindForm(api.loginForm)(
          err =>
            negotiate(
              Unauthorized.page(views.auth.login(err, referrer, isRemember)),
              Unauthorized(doubleJsonFormErrorBody(err))
            ),
          (login, pass) =>
            LoginRateLimit(login.normalize, ctx.req): chargeLimiters =>
              env.security.pwned(pass).foreach { _.so(chargeLimiters()) }
              val isEmail  = EmailAddress.isValid(login.value)
              val stuffing = ctx.req.headers.get("X-Stuffing") | "no" // from nginx
              api.loadLoginForm(login).flatMap {
                _.bindFromRequest()
                  .fold(
                    err =>
                      chargeLimiters()
                      lila.mon.security.login
                        .attempt(isEmail, stuffing = stuffing, result = false)
                        .increment()
                      negotiate(
                        err.errors match
                          case List(FormError("", Seq(err), _)) if is2fa(err) => Ok(err)
                          case _ => Unauthorized.page(views.auth.login(err, referrer, isRemember))
                        ,
                        Unauthorized(doubleJsonFormErrorBody(err))
                      )
                    ,
                    result =>
                      result.toOption match
                        case None => InternalServerError("Authentication error")
                        case Some(u) if u.enabled.no =>
                          negotiate(
                            env.mod.logApi.closedByTeacher(u).flatMap {
                              if _ then
                                authenticateAppealUser(u, redirectTo, routes.Appeal.closedByTeacher.url)
                              else
                                env.mod.logApi.closedByMod(u).flatMap {
                                  if _ then authenticateAppealUser(u, redirectTo, routes.Appeal.landing.url)
                                  else redirectTo(routes.Account.reopen.url)
                                }
                            },
                            Unauthorized(jsonError("This account is closed."))
                          )
                        case Some(u) =>
                          lila.mon.security.login.attempt(isEmail, stuffing = stuffing, result = true)
                          env.user.repo.email(u.id).foreach(_.foreach(garbageCollect(u)))
                          authenticateUser(u, isRemember, Some(redirectTo))
                  )
              }
        )

  def logout = Open:
    val currentSessionId = ~env.security.api.reqSessionId(ctx.req)
    for
      oidc <- env.security.store.oidcSession(currentSessionId)
      endSession <- oidc.so: session =>
        env.oidc.api.endSessionUrl(
          session.idToken,
          s"${env.net.baseUrl}${routes.Lobby.home.url}"
        )
      _   <- env.security.store.delete(currentSessionId)
      _   <- env.push.webSubscriptionApi.unsubscribeBySession(currentSessionId)
      res <- negotiate(Redirect(endSession | routes.Auth.login.url), jsonOkResult)
    yield res.withCookies(env.security.lilaCookie.newSession)

  // mobile app BC logout with GET
  def logoutGet = Auth { ctx ?=> _ ?=>
    negotiate(
      html = Ok.page(views.auth.logout),
      json =
        ctx.req.session.get(api.sessionIdKey).foreach(env.security.store.delete)
        jsonOkResult.withCookies(env.security.lilaCookie.newSession)
    )
  }

  def signup     = Open(oidcLogin)
  def signupLang = LangPage(routes.Auth.signup)(oidcLogin)
  private def serveSignup(using Context) = NoTor:
    forms.signup.website.flatMap: form =>
      Ok.page(views.auth.signup(form))

  private def authLog(user: UserName, email: Option[EmailAddress], msg: String) =
    lila.log("auth").info(s"$user ${email.fold("-")(_.value)} $msg")

  def signupPost = localOpenBody:
    NoTor:
      Firewall:
        forms.preloadEmailDns() >> negotiateApi(
          html = env.security.signup
            .website(ctx.blind)
            .flatMap:
              case Signup.Result.RateLimited => rateLimited
              case Signup.Result.MissingCaptcha =>
                forms.signup.website.flatMap: form =>
                  BadRequest.page(views.auth.signup(form))
              case Signup.Result.Bad(err) =>
                forms.signup.website.flatMap: baseForm =>
                  BadRequest.page(views.auth.signup(baseForm.withForm(err)))
              case Signup.Result.ConfirmEmail(user, email) =>
                Redirect(routes.Auth.checkYourEmail).withCookies(
                  EmailConfirm.cookie
                    .make(env.security.lilaCookie, user, email)(using ctx.req)
                )
              case Signup.Result.AllSet(user, email) =>
                welcome(user, email, sendWelcomeEmail = true) >> redirectNewUser(user)
          ,
          api = apiVersion =>
            env.security
              .ip2proxy(ctx.ip)
              .flatMap: proxy =>
                if proxy.isSafeish
                then
                  env.security.signup
                    .mobile(apiVersion)
                    .flatMap:
                      case Signup.Result.RateLimited        => rateLimited
                      case Signup.Result.MissingCaptcha     => BadRequest(jsonError("Missing captcha?!"))
                      case Signup.Result.Bad(err)           => doubleJsonFormError(err)
                      case Signup.Result.ConfirmEmail(_, _) => Ok(Json.obj("email_confirm" -> true))
                      case Signup.Result.AllSet(user, email) =>
                        welcome(user, email, sendWelcomeEmail = true) >> authenticateUser(
                          user,
                          remember = true
                        )
                else Forbidden(jsonError("This network cannot create new accounts."))
        )

  private def welcome(user: UserModel, email: EmailAddress, sendWelcomeEmail: Boolean)(using
      ctx: Context
  ): Funit =
    garbageCollect(user)(email)
    if sendWelcomeEmail then env.mailer.automaticEmail.welcomeEmail(user, email)
    env.mailer.automaticEmail.welcomePM(user)
    env.pref.api.saveNewUserPrefs(user, ctx.req)

  private def garbageCollect(user: UserModel)(email: EmailAddress)(using ctx: Context) =
    env.security.garbageCollector.delay(user, email, ctx.req, quickly = lila.web.AnnounceApi.get.isDefined)

  def checkYourEmail = localOpen:
    RedirectToProfileIfLoggedIn:
      EmailConfirm.cookie.get(ctx.req) match
        case None => Ok.async(accountC.renderCheckYourEmail)
        case Some(userEmail) =>
          env.user.repo.exists(userEmail.id).flatMap {
            if _ then Ok.async(accountC.renderCheckYourEmail)
            else Redirect(routes.Auth.signup).withCookies(env.security.lilaCookie.newSession)
          }

  // after signup and before confirmation
  def fixEmail = localOpenBody:
    EmailConfirm.cookie.get(ctx.req).so { userEmail =>
      forms.preloadEmailDns() >>
        bindForm(forms.fixEmail(userEmail.email))(
          err => BadRequest.page(views.auth.checkYourEmail(userEmail.email.some, err.some)),
          email =>
            env.user.repo
              .byId(userEmail.id)
              .flatMap:
                _.fold(Redirect(routes.Auth.signup).toFuccess): user =>
                  env.user.repo
                    .mustConfirmEmail(user.id)
                    .flatMap:
                      if _ then
                        val newUserEmail = userEmail.copy(email = email)
                        EmailConfirmRateLimit(newUserEmail, ctx.req, rateLimited):
                          lila.mon.email.send.fix.increment()
                          env.user.repo.setEmail(user.id, newUserEmail.email) >>
                            env.security.emailConfirm
                              .send(user, newUserEmail.email)
                              .inject:
                                Redirect(routes.Auth.checkYourEmail).withCookies:
                                  EmailConfirm.cookie
                                    .make(env.security.lilaCookie, user, newUserEmail.email)(using ctx.req)
                      else Redirect(routes.Auth.login)
        )
    }

  def signupConfirmEmail(token: String) = localOpen:
    env.security.emailConfirm.dryTest(token).flatMap(emailConfirmResult(token))

  def signupConfirmEmailPost(token: String) = localOpen:
    env.security.emailConfirm.confirm(token).flatMap(emailConfirmResult(token))

  private def emailConfirmResult(token: String)(using ctx: Context): EmailConfirm.Result => Fu[Result] =
    case EmailConfirm.Result.NotFound =>
      lila.mon.user.register.confirmEmailResult(false).increment()
      notFound
    case EmailConfirm.Result.NeedsConfirm(user) => Ok.page(views.auth.signupConfirm(user, token, none))
    case EmailConfirm.Result.AlreadyConfirmed(user) =>
      if ctx.is(user) then Redirect(routes.User.show(user.id))
      else Redirect(routes.Auth.login)
    case EmailConfirm.Result.JustConfirmed(user) =>
      lila.mon.user.register.confirmEmailResult(true).increment()
      for
        email <- env.user.repo.email(user.id)
        _ <- email.so: email =>
          authLog(user.username, email.some, s"Confirmed email")
          welcome(user, email, sendWelcomeEmail = false)
        res <- redirectNewUser(user)
      yield res

  private def redirectNewUser(user: UserModel)(using Context) =
    api
      .saveAuthentication(user.id, ctx.mobileApiVersion)
      .flatMap: sessionId =>
        negotiate(
          Redirect(getReferrerOption | routes.User.show(user.id).url)
            .flashSuccess("Welcome! Your account is now active."),
          mobileUserOk(user, sessionId)
        ).map(authenticateCookie(sessionId, remember = true))
      .recoverWith(authRecovery)

  def setFingerPrint(fp: String, ms: Int) = Auth { ctx ?=> me ?=>
    lila.mon.http.fingerPrint.record(ms)
    api
      .setFingerPrint(ctx.req, FingerPrint(fp))
      .logFailure(lila.log("fp"), _ => s"${HTTPRequest.print(ctx.req)} $fp")
      .flatMapz { hash =>
        (!me.lame).so(for
          otherIds <- api.recentUserIdsByFingerHash(hash).map(_.filterNot(_.is(me)))
          _ <- (otherIds.sizeIs >= 2).so(env.user.repo.countLameOrTroll(otherIds).flatMap {
            case nb if nb >= 2 && nb >= otherIds.size / 2 => env.report.api.autoAltPrintReport(me)
            case _                                        => funit
          })
        yield ())
      }
      .inject(NoContent)
  }

  private[controllers] object LoginRateLimit:
    private val lastAttemptIp =
      env.memo.cacheApi.notLoadingSync[UserIdOrEmail, IpAddress](128, "login.lastIp"):
        _.expireAfterWrite(1.minute).build()
    def apply(id: UserIdOrEmail, req: RequestHeader)(run: RateLimit.Charge => Fu[Result])(using
        Context
    ): Fu[Result] =
      val ip          = req.ipAddress
      val multipleIps = lastAttemptIp.asMap().put(id, ip).fold(false)(_ != ip)
      passwordCost(req).flatMap: cost =>
        env.security.passwordHasher.rateLimit[Result](
          rateLimited,
          enforce = env.net.rateLimit,
          ipCost = cost.toInt + EmailAddress.isValid(id.value).so(2),
          userCost = 1 + multipleIps.so(4)
        )(id, req)(run)

  private[controllers] def HasherRateLimit(run: => Fu[Result])(using me: Me, ctx: Context): Fu[Result] =
    passwordCost(req).flatMap: cost =>
      env.security.passwordHasher.rateLimit[Result](
        rateLimited,
        enforce = env.net.rateLimit,
        ipCost = cost.toInt
      )(me.userId.into(UserIdOrEmail), req)(_ => run)

  private def passwordCost(req: RequestHeader): Fu[Float] =
    env.security.ipTrust
      .rateLimitCostFactor(req.ipAddress, _.proxyMultiplier(if HTTPRequest.nginxWhitelist(req) then 1 else 8))

  private[controllers] def EmailConfirmRateLimit = EmailConfirm.rateLimit[Result]

  private[controllers] def RedirectToProfileIfLoggedIn(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.me.fold(f)(me => Redirect(routes.User.show(me.userId)))
