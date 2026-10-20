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
import lila.security.FingerPrint
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

  private def authRecovery(using Context): PartialFunction[Throwable, Fu[Result]] =
    case lila.security.SecurityApi.MustConfirmEmail(_) =>
      BadRequest.page:
        views.site.message("Email confirmation is no longer available")(
          scalatags.Text.all.stringFrag("Please update your email on passport.chess-online.com.")
        )

  def login     = Open(oidcLogin)
  def loginLang = LangPage(routes.Auth.login)(oidcLogin)

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
                Cookie(
                  name = "oidc_state",
                  value = request.getState.getValue,
                  maxAge = Some(600),
                  path = "/",
                  secure = env.net.baseUrl.value.startsWith("https:"),
                  httpOnly = true,
                  sameSite = Some(Cookie.SameSite.Lax)
                )
              )
          }
        }

  def signinOidc = Open(oidcCallback)

  private def oidcCallback(using ctx: Context) = NoBot:
    env.oidc.api
      .authenticate(ctx.req)
      .flatMap {
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
      }
      .map(_.discardingCookies(env.security.lilaCookie.discard("oidc_state")))

  private def oidcRefusalPage(refusal: LoginError)(using Context) =
    import scalatags.Text.all.stringFrag
    refusal match
      case LoginError.Deleted =>
        Forbidden.page(
          views.site.message("Account unavailable")(stringFrag("This account is no longer available."))
        )
      case LoginError.ClosedForever =>
        Forbidden.page(
          views.site.message("Account closed")(stringFrag("This account has been permanently closed."))
        )
      case LoginError.Banned =>
        Forbidden.page(views.site.message("Account closed")(stringFrag("This account is closed.")))
      case LoginError.Closed =>
        Forbidden.page(views.site.message("Account closed")(stringFrag("This account is closed.")))
      case _ =>
        Forbidden.page(views.site.message("Authentication error")(stringFrag("Please try again.")))

  private val is2fa = Set("MissingTotpToken", "InvalidTotpToken")

  def logout = Open:
    val currentSessionId = ~env.security.api.reqSessionId(ctx.req)
    for
      oidc <- env.security.store.oidcSession(currentSessionId)
      endSession <- oidc.so: session =>
        env.oidc.api.endSessionUrl(session.idToken)
      _   <- env.security.store.delete(currentSessionId)
      _   <- env.push.webSubscriptionApi.unsubscribeBySession(currentSessionId)
      res <- negotiate(Redirect(endSession | routes.Auth.login.url), jsonOkResult)
    yield res.withCookies(env.security.lilaCookie.newSession)

  /** IdentityServer's default post-logout redirect. */
  def signoutOidc = Open:
    Redirect(routes.Lobby.home).flashSuccess

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
  private def authLog(user: UserName, email: Option[EmailAddress], msg: String) =
    lila.log("auth").info(s"$user ${email.fold("-")(_.value)} $msg")

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
