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
import lila.security.SecurityForm.{ MagicLink, PasswordReset }
import lila.security.{ FingerPrint, Signup }

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

  def authenticateUser(u: UserModel, remember: Boolean, result: Option[String => Result] = None)(using
      ctx: Context
  ): Fu[Result] =
    api
      .saveAuthentication(u.id, ctx.mobileApiVersion)
      .flatMap: sessionId =>
        negotiate(
          result.fold(Redirect(getReferrer))(_(getReferrer)),
          mobileUserOk(u, sessionId)
        ).map(authenticateCookie(sessionId, remember))
      .recoverWith(authRecovery)

  private def authenticateAppealUser(u: UserModel, redirect: String => Result)(using
      ctx: Context
  ): Fu[Result] =
    api.appeal
      .saveAuthentication(u.id)
      .flatMap: sessionId =>
        authenticateCookie(sessionId, remember = false):
          redirect(routes.Appeal.landing.url)
      .recoverWith(authRecovery)

  private def authenticateCookie(sessionId: String, remember: Boolean)(
      result: Result
  )(using RequestHeader) =
    result.withCookies(
      env.security.lilaCookie.withSession(remember = remember) {
        _ + (api.sessionIdKey -> sessionId) - api.AccessUri - lila.security.EmailConfirm.cookie.name
      }
    )

  private def authRecovery(using ctx: Context): PartialFunction[Throwable, Fu[Result]] =
    case lila.security.SecurityApi.MustConfirmEmail(_) =>
      if HTTPRequest.isXhr(ctx.req)
      then Ok(s"ok:${routes.Auth.checkYourEmail}")
      else BadRequest.async(accountC.renderCheckYourEmail)

  def login     = Open(serveLogin)
  def loginLang = LangPage(routes.Auth.login)(serveLogin)

  private def serveLogin(using ctx: Context) = NoBot:
    val referrer = get("referrer").flatMap(env.web.referrerRedirect.valid)
    val switch   = get("switch").orElse(get("as"))
    referrer.ifTrue(ctx.isAuth).ifTrue(switch.isEmpty) match
      case Some(url) => Redirect(url) // redirect immediately if already logged in
      case None =>
        val prefillUsername = UserStrOrEmail(~switch.filter(_ != "1"))
        val form            = api.loginFormFilled(prefillUsername)
        Ok.page(views.auth.login(form, referrer)).map(_.withCanonical(routes.Auth.login))

  private val is2fa = Set("MissingTotpToken", "InvalidTotpToken")

  def authenticate = OpenBody:
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
                            env.mod.logApi.closedByMod(u).flatMap {
                              if _ then authenticateAppealUser(u, redirectTo)
                              else redirectTo(routes.Account.reopen.url)
                            },
                            Unauthorized(jsonError("This account is closed."))
                          )
                        case Some(u) =>
                          lila.mon.security.login.attempt(isEmail, stuffing = stuffing, result = true)
                          env.user.repo.email(u.id).foreach { _.foreach(garbageCollect(u)) }
                          authenticateUser(u, isRemember, Some(redirectTo))
                  )
              }
        )

  def logout = Open:
    val currentSessionId = ~env.security.api.reqSessionId(ctx.req)
    env.security.store.delete(currentSessionId) >>
      env.push.webSubscriptionApi.unsubscribeBySession(currentSessionId) >>
      negotiate(
        Redirect(routes.Auth.login),
        jsonOkResult
      ).dmap(_.withCookies(env.security.lilaCookie.newSession))

  // mobile app BC logout with GET
  def logoutGet = Auth { ctx ?=> _ ?=>
    negotiate(
      html = Ok.page(views.auth.logout),
      json =
        ctx.req.session.get(api.sessionIdKey).foreach(env.security.store.delete)
        jsonOkResult.withCookies(env.security.lilaCookie.newSession)
    )
  }

  def signup     = Open(serveSignup)
  def signupLang = LangPage(routes.Auth.signup)(serveSignup)
  private def serveSignup(using Context) = NoTor:
    forms.signup.website.flatMap: form =>
      Ok.page(views.auth.signup(form))

  private def authLog(user: UserName, email: Option[EmailAddress], msg: String) =
    lila.log("auth").info(s"$user ${email.fold("-")(_.value)} $msg")

  def signupPost = OpenBody:
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
                  lila.security.EmailConfirm.cookie
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

  def checkYourEmail = Open:
    RedirectToProfileIfLoggedIn:
      lila.security.EmailConfirm.cookie.get(ctx.req) match
        case None => Ok.async(accountC.renderCheckYourEmail)
        case Some(userEmail) =>
          env.user.repo.exists(userEmail.username).flatMap {
            if _ then Ok.async(accountC.renderCheckYourEmail)
            else Redirect(routes.Auth.signup).withCookies(env.security.lilaCookie.newSession)
          }

  // after signup and before confirmation
  def fixEmail = OpenBody:
    lila.security.EmailConfirm.cookie.get(ctx.req).so { userEmail =>
      forms.preloadEmailDns() >>
        bindForm(forms.fixEmail(userEmail.email))(
          err => BadRequest.page(views.auth.checkYourEmail(userEmail.email.some, err.some)),
          email =>
            env.user.repo
              .byId(userEmail.username)
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
                                  lila.security.EmailConfirm.cookie
                                    .make(env.security.lilaCookie, user, newUserEmail.email)(using ctx.req)
                      else Redirect(routes.Auth.login)
        )
    }

  def signupConfirmEmail(token: String) = Open:
    import lila.security.EmailConfirm.Result
    env.security.emailConfirm.confirm(token).flatMap {
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

  private def redirectNewUser(user: UserModel)(using Context) =
    api
      .saveAuthentication(user.id, ctx.mobileApiVersion)
      .flatMap { sessionId =>
        negotiate(
          Redirect(getReferrerOption | routes.User.show(user.username).url)
            .flashSuccess("Welcome! Your account is now active."),
          mobileUserOk(user, sessionId)
        ).map(authenticateCookie(sessionId, remember = true))
      }
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

  private def renderPasswordReset(form: Option[Form[PasswordReset]], fail: Boolean)(using ctx: Context) =
    renderAsync:
      env.security.forms.passwordReset.map: baseForm =>
        views.auth.passwordReset(form.foldLeft(baseForm)(_.withForm(_)), fail)

  def passwordReset = Open:
    renderPasswordReset(none, fail = false).map { Ok(_) }

  def passwordResetApply =
    OpenBody:
      env.security.hcaptcha
        .verify()
        .flatMap: captcha =>
          if captcha.ok
          then
            forms.passwordReset.flatMap {
              _.form
                .bindFromRequest()
                .fold(
                  err => renderPasswordReset(err.some, fail = true).map { BadRequest(_) },
                  data =>
                    env.user.repo.enabledWithEmail(data.email.normalize).flatMap {
                      case Some((user, storedEmail)) =>
                        lila.mon.user.auth.passwordResetRequest("success").increment()
                        env.security.passwordReset
                          .send(user, storedEmail)
                          .inject:
                            Redirect:
                              routes.Auth.passwordResetSent(storedEmail.conceal)
                      case _ =>
                        lila.mon.user.auth.passwordResetRequest("noEmail").increment()
                        Redirect(routes.Auth.passwordResetSent(data.email.conceal))
                    }
                )
            }
          else renderPasswordReset(none, fail = true).map { BadRequest(_) }

  def passwordResetSent(email: String) = Open:
    Ok.page(views.auth.passwordResetSent(email))

  def passwordResetConfirm(token: String) = Open:
    env.security.passwordReset.confirm(token).flatMap {
      case None =>
        lila.mon.user.auth.passwordResetConfirm("tokenFail").increment()
        notFound
      case Some(me) =>
        given Me = me
        authLog(me.username, none, "Reset password")
        lila.mon.user.auth.passwordResetConfirm("tokenOk").increment()
        Ok.page:
          views.auth.passwordResetConfirm(token, forms.passwdResetForMe, none)
    }

  def passwordResetConfirmApply(token: String) = OpenBody:
    env.security.passwordReset.confirm(token).flatMap {
      case None =>
        lila.mon.user.auth.passwordResetConfirm("tokenPostFail").increment()
        notFound
      case Some(me) =>
        given Me = me
        val user = me.value
        FormFuResult(forms.passwdResetForMe) { err =>
          renderPage(views.auth.passwordResetConfirm(token, err, false.some))
        } { data =>
          HasherRateLimit:
            for
              _         <- env.security.authenticator.setPassword(user.id, ClearPassword(data.newPasswd1))
              confirmed <- env.user.repo.setEmailConfirmed(user.id)
              _ <- confirmed.so:
                welcome(user, _, sendWelcomeEmail = false)
              _   <- env.user.repo.disableTwoFactor(user.id)
              _   <- env.security.store.closeAllSessionsOf(user.id)
              _   <- env.push.webSubscriptionApi.unsubscribeByUser(user)
              _   <- env.push.unregisterDevices(user)
              res <- authenticateUser(user, remember = true)
            yield
              lila.mon.user.auth.passwordResetConfirm("success").increment()
              res
        }
    }

  private def renderMagicLink(form: Option[Form[MagicLink]], fail: Boolean)(using Context) =
    env.security.forms.magicLink.map: baseForm =>
      views.auth.magicLink(form.foldLeft(baseForm)(_.withForm(_)), fail)

  def magicLink = Open:
    Firewall:
      Ok.async(renderMagicLink(none, fail = false))

  def magicLinkApply = OpenBody:
    Firewall:
      env.security.hcaptcha.verify().flatMap { captcha =>
        if captcha.ok then
          forms.magicLink.flatMap {
            _.form
              .bindFromRequest()
              .fold(
                err => BadRequest.async(renderMagicLink(err.some, fail = true)),
                data =>
                  env.user.repo.enabledWithEmail(data.email.normalize).flatMap {
                    case Some((user, storedEmail)) =>
                      env.security.magicLink.rateLimit[Result](user, storedEmail, ctx.req, rateLimited):
                        lila.mon.user.auth.magicLinkRequest("success").increment()
                        env.security.magicLink
                          .send(user, storedEmail)
                          .inject(Redirect:
                            routes.Auth.magicLinkSent
                          )
                    case _ =>
                      lila.mon.user.auth.magicLinkRequest("no_email").increment()
                      Redirect(routes.Auth.magicLinkSent)
                  }
              )
          }
        else BadRequest.async(renderMagicLink(none, fail = true))
      }

  def magicLinkSent = Open:
    Ok.page(views.auth.magicLinkSent)

  def magicLinkLogin(token: String) = Open:
    if ctx.isAuth
    then Redirect(routes.Lobby.home)
    else
      Firewall:
        limit.magicLink(token, rateLimited):
          env.security.magicLink.confirm(token).flatMap {
            case None =>
              lila.mon.user.auth.magicLinkConfirm("token_fail").increment()
              notFound
            case Some(user) =>
              authLog(user.username, none, "Magic link")
              for
                result <- authenticateUser(user, remember = true)
                _ = lila.mon.user.auth.magicLinkConfirm("success").increment()
              yield result
          }

  def makeLoginToken = AuthOrScoped(_.Web.Login) { ctx ?=> me ?=>
    if ctx.isOAuth
    then lila.log("oauth").info(s"api makeLoginToken ${me.username} ${HTTPRequest.printClient(ctx.req)}")
    JsonOk:
      env.security.loginToken
        .generate(me.value)
        .map: token =>
          Json.obj(
            "userId" -> me.userId,
            "url"    -> s"${env.net.baseUrl}${routes.Auth.loginWithToken(token).url}"
          )
  }

  def loginWithToken(token: String) = Open:
    if ctx.isAuth
    then Redirect(getReferrer)
    else
      Firewall:
        consumingToken(token): user =>
          Ok.async:
            env.security.loginToken.generate(user).map {
              views.auth.tokenLoginConfirmation(user, _, get("referrer"))
            }

  def loginWithTokenPost(token: String, referrer: Option[String]) =
    Open:
      if ctx.isAuth
      then Redirect(getReferrer)
      else
        Firewall:
          consumingToken(token) { authenticateUser(_, remember = true) }

  private def consumingToken(token: String)(f: UserModel => Fu[Result])(using Context) =
    env.security.loginToken.consume(token).flatMap {
      case None =>
        BadRequest.page:
          import scalatags.Text.all.stringFrag
          views.site.message("This token has expired.")(stringFrag("Please go back and try again."))
      case Some(user) => f(user)
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

  private[controllers] def EmailConfirmRateLimit = lila.security.EmailConfirm.rateLimit[Result]

  private[controllers] def RedirectToProfileIfLoggedIn(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.me.fold(f)(me => Redirect(routes.User.show(me.username)))
