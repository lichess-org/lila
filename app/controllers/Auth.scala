package controllers

import alleycats.Zero
import play.api.data.FormError
import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.api.WebContext
import lila.app.{ given, * }
import lila.common.{ EmailAddress, HTTPRequest, IpAddress }
import lila.memo.RateLimit
import lila.security.SecurityForm.{ MagicLink, PasswordReset }
import lila.security.{ FingerPrint, Signup }
import lila.user.User.ClearPassword
import lila.user.{ PasswordHasher, User as UserModel }

final class Auth(
    env: Env,
    accountC: => Account
) extends LilaController(env):

  private def api   = env.security.api
  private def forms = env.security.forms

  private def mobileUserOk(u: UserModel, sessionId: String)(using WebContext): Fu[Result] =
    env.round.proxyRepo urgentGames u map { povs =>
      Ok:
        env.user.jsonView.full(
          u,
          withRating = ctx.pref.showRatings,
          withProfile = true
        ) ++ Json.obj(
          "nowPlaying" -> JsArray(povs take 20 map env.api.lobbyApi.nowPlaying),
          "sessionId"  -> sessionId
        )
    }

  private def getReferrerOption(using ctx: WebContext): Option[String] =
    get("referrer").flatMap(env.api.referrerRedirect.valid) orElse
      ctx.req.session.get(api.AccessUri)

  private def getReferrer(using WebContext): String = getReferrerOption | routes.Lobby.home.url

  def authenticateUser(u: UserModel, remember: Boolean, result: Option[String => Result] = None)(using
      ctx: WebContext
  ): Fu[Result] =
    api.saveAuthentication(u.id, ctx.mobileApiVersion) flatMap { sessionId =>
      negotiate(
        html = result.fold(Redirect(getReferrer))(_(getReferrer)),
        api = _ => mobileUserOk(u, sessionId)
      ) map authenticateCookie(sessionId, remember)
    } recoverWith authRecovery

  private def authenticateAppealUser(u: UserModel, redirect: String => Result)(using
      ctx: WebContext
  ): Fu[Result] =
    api.appeal.saveAuthentication(u.id) flatMap { sessionId =>
      negotiate(
        html = authenticateCookie(sessionId, remember = false):
          redirect(appeal.routes.Appeal.landing.url)
        ,
        api = _ => NotFound
      )
    } recoverWith authRecovery

  private def authenticateCookie(sessionId: String, remember: Boolean)(
      result: Result
  )(using RequestHeader) =
    result.withCookies(
      env.lilaCookie.withSession(remember = remember) {
        _ + (api.sessionIdKey -> sessionId) - api.AccessUri - lila.security.EmailConfirm.cookie.name
      }
    )

  private def authRecovery(using ctx: WebContext): PartialFunction[Throwable, Fu[Result]] =
    case lila.security.SecurityApi.MustConfirmEmail(_) =>
      if HTTPRequest isXhr ctx.req
      then Ok(s"ok:${routes.Auth.checkYourEmail}")
      else BadRequest(accountC.renderCheckYourEmail)

  def login     = Open(serveLogin)
  def loginLang = LangPage(routes.Auth.login)(serveLogin)

  private def serveLogin(using ctx: WebContext) = NoBot {
    val referrer = get("referrer") flatMap env.api.referrerRedirect.valid
    val switch   = get("switch")
    referrer ifTrue ctx.isAuth ifTrue switch.isEmpty match
      case Some(url) => Redirect(url) // redirect immediately if already logged in
      case None =>
        val prefillUsername = lila.security.UserStrOrEmail(~switch.filter(_ != "1"))
        val form            = api.loginFormFilled(prefillUsername)
        Ok(html.auth.login(form, referrer)).withCanonical(routes.Auth.login)
  }

  private val is2fa = Set("MissingTotpToken", "InvalidTotpToken")

  def authenticate = OpenBody:
    NoCrawlers:
      Firewall:
        def redirectTo(url: String) = if (HTTPRequest isXhr ctx.req) Ok(s"ok:$url") else Redirect(url)
        val referrer                = get("referrer").filterNot(env.api.referrerRedirect.sillyLoginReferrers)
        api.loginForm
          .bindFromRequest()
          .fold(
            err =>
              negotiate(
                html = Unauthorized(html.auth.login(err, referrer)),
                api = _ => Unauthorized(ridiculousBackwardCompatibleJsonError(errorsAsJson(err)))
              ),
            (login, pass) =>
              LoginRateLimit(login.normalize, ctx.req): chargeLimiters =>
                env.security.pwned(pass) foreach { _ so chargeLimiters() }
                val isEmail  = EmailAddress.isValid(login.value)
                val stuffing = ctx.req.headers.get("X-Stuffing") | "no" // from nginx
                api.loadLoginForm(login) flatMap {
                  _.bindFromRequest()
                    .fold(
                      err =>
                        chargeLimiters()
                        lila.mon.security.login
                          .attempt(isEmail, stuffing = stuffing, result = false)
                          .increment()
                        negotiate(
                          html = err.errors match
                            case List(FormError("", Seq(err), _)) if is2fa(err) => Ok(err)
                            case _ => Unauthorized(html.auth.login(err, referrer))
                          ,
                          api = _ =>
                            Unauthorized:
                              ridiculousBackwardCompatibleJsonError(errorsAsJson(err))
                        )
                      ,
                      result =>
                        result.toOption match
                          case None => InternalServerError("Authentication error")
                          case Some(u) if u.enabled.no =>
                            negotiate(
                              html = env.mod.logApi.closedByMod(u) flatMap {
                                if _ then authenticateAppealUser(u, redirectTo)
                                else redirectTo(routes.Account.reopen.url)
                              },
                              api = _ => Unauthorized(jsonError("This account is closed."))
                            )
                          case Some(u) =>
                            lila.mon.security.login.attempt(isEmail, stuffing = stuffing, result = true)
                            env.user.repo.email(u.id) foreach { _ foreach garbageCollect(u) }
                            val remember = api.rememberForm.bindFromRequest().value | true
                            authenticateUser(u, remember, Some(redirectTo))
                    )
                }
          )

  def logout = Open:
    val currentSessionId = ~env.security.api.reqSessionId(ctx.req)
    env.security.store.delete(currentSessionId) >>
      env.push.webSubscriptionApi.unsubscribeBySession(currentSessionId) >>
      negotiate(
        html = Redirect(routes.Auth.login),
        api = _ => Ok(Json.obj("ok" -> true))
      ).dmap(_.withCookies(env.lilaCookie.newSession))

  // mobile app BC logout with GET
  def logoutGet = Auth { ctx ?=> _ =>
    negotiate(
      html = Ok(html.auth.bits.logout()),
      api = _ =>
        ctx.req.session get api.sessionIdKey foreach env.security.store.delete
        Ok(Json.obj("ok" -> true)).withCookies(env.lilaCookie.newSession)
    )
  }

  def signup     = Open(serveSignup)
  def signupLang = LangPage(routes.Auth.signup)(serveSignup)
  private def serveSignup(using WebContext) = NoTor:
    forms.signup.website.map: form =>
      Ok(html.auth.signup(form))

  private def authLog(user: UserName, email: Option[EmailAddress], msg: String) =
    lila.log("auth").info(s"$user ${email.fold("-")(_.value)} $msg")

  def signupPost = OpenBody:
    NoTor:
      Firewall:
        forms.preloadEmailDns() >> negotiate(
          html = env.security.signup
            .website(ctx.blind)
            .flatMap {
              case Signup.Result.RateLimited => limitedDefault.zero
              case Signup.Result.MissingCaptcha =>
                forms.signup.website.map: form =>
                  BadRequest(html.auth.signup(form))
              case Signup.Result.Bad(err) =>
                forms.signup.website.map: baseForm =>
                  BadRequest(html.auth.signup(baseForm withForm err))
              case Signup.Result.ConfirmEmail(user, email) =>
                Redirect(routes.Auth.checkYourEmail) withCookies
                  lila.security.EmailConfirm.cookie
                    .make(env.lilaCookie, user, email)(using ctx.req)
              case Signup.Result.AllSet(user, email) =>
                welcome(user, email, sendWelcomeEmail = true) >> redirectNewUser(user)
            },
          api = apiVersion =>
            env.security.signup
              .mobile(apiVersion)
              .flatMap {
                case Signup.Result.RateLimited        => limitedDefault.zero
                case Signup.Result.MissingCaptcha     => BadRequest(jsonError("Missing captcha?!"))
                case Signup.Result.Bad(err)           => jsonFormError(err)
                case Signup.Result.ConfirmEmail(_, _) => Ok(Json.obj("email_confirm" -> true))
                case Signup.Result.AllSet(user, email) =>
                  welcome(user, email, sendWelcomeEmail = true) >> authenticateUser(user, remember = true)
              }
        )

  private def welcome(user: UserModel, email: EmailAddress, sendWelcomeEmail: Boolean)(using
      ctx: WebContext
  ): Funit =
    garbageCollect(user)(email)
    if (sendWelcomeEmail) env.mailer.automaticEmail.welcomeEmail(user, email)
    env.mailer.automaticEmail.welcomePM(user)
    env.pref.api.saveNewUserPrefs(user, ctx.req)

  private def garbageCollect(user: UserModel)(email: EmailAddress)(using ctx: WebContext) =
    env.security.garbageCollector.delay(user, email, ctx.req)

  def checkYourEmail = Open:
    RedirectToProfileIfLoggedIn:
      lila.security.EmailConfirm.cookie get ctx.req match
        case None => Ok(accountC.renderCheckYourEmail)
        case Some(userEmail) =>
          env.user.repo exists userEmail.username map {
            if _ then Ok(accountC.renderCheckYourEmail)
            else Redirect(routes.Auth.signup) withCookies env.lilaCookie.newSession(using ctx.req)
          }

  // after signup and before confirmation
  def fixEmail = OpenBody:
    lila.security.EmailConfirm.cookie.get(ctx.req) so { userEmail =>
      forms.preloadEmailDns() >> forms
        .fixEmail(userEmail.email)
        .bindFromRequest()
        .fold(
          err => BadRequest(html.auth.checkYourEmail(userEmail.some, err.some)),
          email =>
            env.user.repo.byId(userEmail.username) flatMap {
              _.fold(Redirect(routes.Auth.signup).toFuccess): user =>
                env.user.repo.mustConfirmEmail(user.id) flatMap {
                  case false => Redirect(routes.Auth.login)
                  case _ =>
                    val newUserEmail = userEmail.copy(email = email)
                    EmailConfirmRateLimit(newUserEmail, ctx.req, rateLimitedFu):
                      lila.mon.email.send.fix.increment()
                      env.user.repo.setEmail(user.id, newUserEmail.email) >>
                        env.security.emailConfirm
                          .send(user, newUserEmail.email)
                          .inject:
                            Redirect(routes.Auth.checkYourEmail).withCookies:
                              lila.security.EmailConfirm.cookie
                                .make(env.lilaCookie, user, newUserEmail.email)(using ctx.req)
                }
            }
        )
    }

  def signupConfirmEmail(token: String) = Open:
    import lila.security.EmailConfirm.Result
    env.security.emailConfirm.confirm(token) flatMap {
      case Result.NotFound =>
        lila.mon.user.register.confirmEmailResult(false).increment()
        notFound
      case Result.AlreadyConfirmed(user) if ctx.is(user) =>
        Redirect(routes.User.show(user.username))
      case Result.AlreadyConfirmed(_) =>
        Redirect(routes.Auth.login)
      case Result.JustConfirmed(user) =>
        lila.mon.user.register.confirmEmailResult(true).increment()
        env.user.repo.email(user.id).flatMap {
          _.so: email =>
            authLog(user.username, email.some, s"Confirmed email ${email.value}")
            welcome(user, email, sendWelcomeEmail = false)
        } >> redirectNewUser(user)
    }

  private def redirectNewUser(user: UserModel)(using WebContext) =
    api.saveAuthentication(user.id, ctx.mobileApiVersion) flatMap { sessionId =>
      negotiate(
        html = Redirect(getReferrerOption | routes.User.show(user.username).url),
        api = _ => mobileUserOk(user, sessionId)
      ) map authenticateCookie(sessionId, remember = true)
    } recoverWith authRecovery

  def setFingerPrint(fp: String, ms: Int) = Auth { ctx ?=> me =>
    lila.mon.http.fingerPrint.record(ms)
    api
      .setFingerPrint(ctx.req, FingerPrint(fp))
      .logFailure(lila log "fp", _ => s"${HTTPRequest print ctx.req} $fp") flatMapz { hash =>
      !me.lame so (for
        otherIds <- api.recentUserIdsByFingerHash(hash).map(_.filter(me.id.!=))
        _ <- (otherIds.sizeIs >= 2) so env.user.repo.countLameOrTroll(otherIds).flatMap {
          case nb if nb >= 2 && nb >= otherIds.size / 2 => env.report.api.autoAltPrintReport(me.id)
          case _                                        => funit
        }
      yield ())
    } inject NoContent
  }

  private def renderPasswordReset(form: Option[play.api.data.Form[PasswordReset]], fail: Boolean)(using
      ctx: WebContext
  ) =
    env.security.forms.passwordReset map { baseForm =>
      html.auth.bits.passwordReset(form.foldLeft(baseForm)(_ withForm _), fail)
    }

  def passwordReset = Open:
    renderPasswordReset(none, fail = false) map { Ok(_) }

  def passwordResetApply =
    OpenBody:
      env.security.hcaptcha
        .verify()
        .flatMap: captcha =>
          if captcha.ok
          then
            forms.passwordReset flatMap {
              _.form
                .bindFromRequest()
                .fold(
                  err => renderPasswordReset(err.some, fail = true) map { BadRequest(_) },
                  data =>
                    env.user.repo.enabledWithEmail(data.email.normalize) flatMap {
                      case Some((user, storedEmail)) =>
                        lila.mon.user.auth.passwordResetRequest("success").increment()
                        env.security.passwordReset.send(user, storedEmail) inject Redirect(
                          routes.Auth.passwordResetSent(storedEmail.conceal)
                        )
                      case _ =>
                        lila.mon.user.auth.passwordResetRequest("noEmail").increment()
                        Redirect(routes.Auth.passwordResetSent(data.email.conceal))
                    }
                )
            }
          else renderPasswordReset(none, fail = true) map { BadRequest(_) }

  def passwordResetSent(email: String) = Open:
    html.auth.bits.passwordResetSent(email)

  def passwordResetConfirm(token: String) = Open:
    env.security.passwordReset confirm token flatMap {
      case None =>
        lila.mon.user.auth.passwordResetConfirm("tokenFail").increment()
        notFound
      case Some(user) =>
        authLog(user.username, none, "Reset password")
        lila.mon.user.auth.passwordResetConfirm("tokenOk").increment()
        html.auth.bits.passwordResetConfirm(user, token, forms.passwdResetFor(user), none)
    }

  def passwordResetConfirmApply(token: String) = OpenBody:
    env.security.passwordReset confirm token flatMap {
      case None =>
        lila.mon.user.auth.passwordResetConfirm("tokenPostFail").increment()
        notFound
      case Some(user) =>
        FormFuResult(forms.passwdResetFor(user)) { err =>
          fuccess(html.auth.bits.passwordResetConfirm(user, token, err, false.some))
        } { data =>
          HasherRateLimit(user.id, ctx.req):
            env.user.authenticator.setPassword(user.id, ClearPassword(data.newPasswd1)) >>
              env.user.repo.setEmailConfirmed(user.id).flatMapz {
                welcome(user, _, sendWelcomeEmail = false)
              } >>
              env.user.repo.disableTwoFactor(user.id) >>
              env.security.store.closeAllSessionsOf(user.id) >>
              env.push.webSubscriptionApi.unsubscribeByUser(user) >>
              env.push.unregisterDevices(user) >>
              authenticateUser(user, remember = true) >>-
              lila.mon.user.auth.passwordResetConfirm("success").increment().unit
        }
    }

  private def renderMagicLink(form: Option[play.api.data.Form[MagicLink]], fail: Boolean)(using
      ctx: WebContext
  ) =
    env.security.forms.magicLink map { baseForm =>
      html.auth.bits.magicLink(form.foldLeft(baseForm)(_ withForm _), fail)
    }

  def magicLink = Open:
    Firewall:
      renderMagicLink(none, fail = false) map { Ok(_) }

  def magicLinkApply = OpenBody:
    Firewall:
      env.security.hcaptcha.verify() flatMap { captcha =>
        if (captcha.ok)
          forms.magicLink flatMap {
            _.form
              .bindFromRequest()
              .fold(
                err => renderMagicLink(err.some, fail = true) map { BadRequest(_) },
                data =>
                  env.user.repo.enabledWithEmail(data.email.normalize) flatMap {
                    case Some((user, storedEmail)) =>
                      MagicLinkRateLimit(user, storedEmail, ctx.req, rateLimitedFu):
                        lila.mon.user.auth.magicLinkRequest("success").increment()
                        env.security.magicLink.send(user, storedEmail) inject Redirect:
                          routes.Auth.magicLinkSent
                    case _ =>
                      lila.mon.user.auth.magicLinkRequest("no_email").increment()
                      Redirect(routes.Auth.magicLinkSent)
                  }
              )
          }
        else
          renderMagicLink(none, fail = true) map { BadRequest(_) }
      }

  def magicLinkSent = Open:
    html.auth.bits.magicLinkSent

  private lazy val magicLinkLoginRateLimitPerToken = RateLimit[String](
    credits = 3,
    duration = 1 hour,
    key = "login.magicLink.token"
  )

  def magicLinkLogin(token: String) = Open:
    if ctx.isAuth
    then Redirect(routes.Lobby.home)
    else
      Firewall:
        magicLinkLoginRateLimitPerToken(token, rateLimitedFu):
          env.security.magicLink confirm token flatMap {
            case None =>
              lila.mon.user.auth.magicLinkConfirm("token_fail").increment()
              notFound
            case Some(user) =>
              authLog(user.username, none, "Magic link")
              authenticateUser(user, remember = true) >>-
                lila.mon.user.auth.magicLinkConfirm("success").increment().unit
          }

  def makeLoginToken =
    def loginTokenFor(me: UserModel) = JsonOk:
      env.security.loginToken generate me map { token =>
        Json.obj(
          "userId" -> me.id,
          "url"    -> s"${env.net.baseUrl}${routes.Auth.loginWithToken(token).url}"
        )
      }
    AuthOrScoped(_.Web.Login)(
      _ ?=> loginTokenFor,
      ctx ?=>
        user =>
          lila.log("oauth").info(s"api makeLoginToken ${user.id} ${HTTPRequest printClient ctx.req}")
          loginTokenFor(user)
    )

  def loginWithToken(token: String) = Open:
    if ctx.isAuth
    then Redirect(getReferrer)
    else
      Firewall:
        consumingToken(token): user =>
          env.security.loginToken.generate(user) map { newToken =>
            Ok(html.auth.bits.tokenLoginConfirmation(user, newToken, get("referrer")))
          }

  def loginWithTokenPost(token: String, referrer: Option[String]) =
    Open:
      if ctx.isAuth
      then Redirect(getReferrer)
      else
        Firewall:
          consumingToken(token) { authenticateUser(_, remember = true) }

  private def consumingToken(token: String)(f: UserModel => Fu[Result])(using WebContext) =
    env.security.loginToken consume token flatMap {
      case None =>
        BadRequest:
          import scalatags.Text.all.stringFrag
          html.site.message("This token has expired.")(stringFrag("Please go back and try again."))
      case Some(user) => f(user)
    }

  private given limitedDefault: Zero[Result] = Zero(rateLimited)

  private[controllers] object LoginRateLimit:
    private val lastAttemptIp =
      env.memo.cacheApi.notLoadingSync[UserIdOrEmail, IpAddress](64, "login.lastIp"):
        _.expireAfterWrite(10.seconds).build()
    def apply(id: UserIdOrEmail, req: RequestHeader)(run: RateLimit.Charge => Fu[Result]): Fu[Result] =
      val ip          = req.ipAddress
      val multipleIps = lastAttemptIp.asMap().put(id, ip).fold(false)(_ != ip)
      env.security.ipTrust
        .isSuspicious(ip)
        .flatMap: ipSusp =>
          PasswordHasher.rateLimit[Result](
            rateLimitedFu,
            enforce = env.net.rateLimit,
            ipCost = 1 + ipSusp.so(15) + EmailAddress.isValid(id.value).so(2),
            userCost = 1 + multipleIps.so(4)
          )(id, req)(run)

  private[controllers] def HasherRateLimit(id: UserId, req: RequestHeader)(run: => Fu[Result]): Fu[Result] =
    env.security
      .ip2proxy(req.ipAddress)
      .flatMap: proxy =>
        PasswordHasher.rateLimit[Result](
          rateLimitedFu,
          enforce = env.net.rateLimit,
          ipCost = if proxy.is then 10 else 1
        )(id into UserIdOrEmail, req)(_ => run)

  private[controllers] def EmailConfirmRateLimit = lila.security.EmailConfirm.rateLimit[Result]

  private[controllers] def MagicLinkRateLimit = lila.security.MagicLink.rateLimit[Result]

  private[controllers] def RedirectToProfileIfLoggedIn(f: => Fu[Result])(using ctx: WebContext): Fu[Result] =
    ctx.me match
      case Some(me) => Redirect(routes.User.show(me.username))
      case None     => f
