package controllers
import play.api.data.Form
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.id.SessionId
import lila.core.email.{ UserIdOrEmail, UserStrOrEmail }
import lila.core.net.ValidReferrer
import lila.core.security.ClearPassword
import lila.core.misc.AuthCustomUi
import lila.memo.RateLimit
import lila.oauth.OAuthSignedClient.SimpleSignup
import lila.security.SecurityForm.{ MagicLink, PasswordReset }
import lila.security.{ FingerPrint, Signup, EmailConfirm, IsPwned }

final class Auth(env: Env, accountC: => Account) extends LilaController(env):

  import env.security.{ api, forms }
  def logger = lila.security.loggerAuth

  private given (using Context): Option[ValidReferrer] = env.web.referrerRedirect.fromReq

  private def referrerOr(default: => Call)(using referrer: Option[ValidReferrer]): String =
    referrer.fold(default.url)(_.value)

  def authenticateUser(
      u: UserModel,
      pwned: IsPwned,
      remember: Boolean,
      result: => Option[Result] = None
  )(using ctx: Context): Fu[Result] = {
    for
      sessionId <- api.saveAuthentication(u.id, ctx.mobileApiVersion, pwned)
      res <- negotiate(
        result | Redirect(referrerOr(routes.Lobby.home)),
        for
          povs <- env.round.proxyRepo.urgentGames(u)
          perfs <- ctx.pref.showRatings.optionFu(env.user.perfsRepo.perfsOf(u))
          _ <- env.msg.systemMsg.lichobileDeprecationMessage(u)
        yield Ok:
          env.user.jsonView.full(u, perfs, withProfile = true) ++ Json.obj(
            "nowPlaying" -> JsArray(povs.value.take(20).map(env.api.lobbyApi.nowPlaying)),
            "sessionId" -> sessionId
          )
      ).map(authenticateCookie(sessionId, remember))
    yield res
  }.recoverWith(authRecovery)

  private def authenticateAppealUser(
      u: UserModel,
      redirect: String => Result,
      url: Call = routes.Appeal.landing
  )(using
      ctx: Context
  ): Fu[Result] =
    api.appeal
      .saveAuthentication(u.id)
      .flatMap: sessionId =>
        authenticateCookie(sessionId, remember = false):
          redirect(url.url)
      .recoverWith(authRecovery)

  private def authenticateCookie(sessionId: SessionId, remember: Boolean)(
      result: Result
  )(using RequestHeader) =
    result.withCookies(
      env.security.lilaCookie.withSession(remember = remember) {
        _ + (api.sessionIdKey -> sessionId.value) - EmailConfirm.cookie.name
      }
    )

  private def authRecovery(using ctx: Context): PartialFunction[Throwable, Fu[Result]] =
    case lila.security.SecurityApi.MustConfirmEmail(_) =>
      if HTTPRequest.isXhr(ctx.req)
      then Ok(s"ok:${routes.Auth.checkYourEmail}")
      else BadRequest.async(accountC.renderCheckYourEmail)

  def login = Open(serveLogin)
  def loginTakex3 = Open(serveLogin(takex3CustomUi, routes.Auth.loginTakex3))
  def loginLang = LangPage(routes.Auth.login)(serveLogin)

  private def takex3CustomUi = env.oAuth.signedClients.takex3.design
  private val takex3PasswordResetSource = "takex3"

  private def passwordResetSource(using RequestHeader): Option[String] =
    get("source").filter(_ == takex3PasswordResetSource)

  private def passwordResetCustomUi(using RequestHeader): Option[AuthCustomUi] =
    if passwordResetSource.isDefined then takex3CustomUi else none

  private def customUiOrDefault(customUi: Option[AuthCustomUi])(using
      Option[ValidReferrer]
  ): Option[AuthCustomUi] =
    customUi.orElse(simpleSignup.flatMap(_.client.design))

  private def serveLogin(using ctx: Context, referrer: Option[ValidReferrer]): Fu[Result] =
    serveLogin(none, routes.Auth.login)

  private def serveLogin(
      customUi: Option[AuthCustomUi],
      canonical: Call
  )(using ctx: Context, referrer: Option[ValidReferrer]) = NoBot:
    val switch = get("switch").orElse(get("as"))
    t3Counter(_.login.load)
    referrer.ifTrue(ctx.isAuth).ifTrue(switch.isEmpty) match
      case Some(url) =>
        t3Counter(_.login.success)
        Redirect(url.value) // redirect immediately if already logged in
      case None =>
        val prefillUsername = UserStrOrEmail(~switch.filter(_ != "1"))
        val form = api.loginFormFilled(prefillUsername)
        given Option[AuthCustomUi] = customUiOrDefault(customUi)
        val page =
          if isTakex3(customUi) then views.auth.loginTakex3(form)
          else views.auth.login(form)
        Ok.page(page).map(_.withCanonical(canonical))

  def authenticate = OpenBody:
    serveAuthenticate()

  def authenticateTakex3 = OpenBody:
    serveAuthenticate(takex3CustomUi)

  private def serveAuthenticate(customUi: Option[AuthCustomUi] = None)(using BodyContext[?]) =
    given Option[AuthCustomUi] = customUiOrDefault(customUi)
    def loginPage(form: Form[?], isRemember: Boolean) =
      if isTakex3(customUi) then views.auth.loginTakex3(form, isRemember)
      else views.auth.login(form, isRemember)
    NoCrawlers:
      Firewall:
        def redirectTo(url: String) = if HTTPRequest.isXhr(ctx.req) then Ok(s"ok:$url") else Redirect(url)
        val isRemember = api.rememberForm.bindFromRequest().value | true
        val isLichobile = HTTPRequest.isLichobile(ctx.req)
        if isLichobile && !env.security.lichobileLogin.get() then
          BadRequest(Json.obj("global" -> List("Please use our new mobile app! lichess.org/app")))
        else
          bindForm(api.loginForm)(
            err =>
              negotiate(
                Unauthorized.page(loginPage(err, isRemember)),
                Unauthorized(doubleJsonFormErrorBody(err))
              ),
            loginData =>
              val turnstileResult = fuccess(isLichobile) >>|
                env.security.turnstileCookie.test(loginData) >>|
                env.security.turnstile.verify()
              turnstileResult.flatMap:
                if _ then
                  LoginRateLimit(loginData.username.normalize, ctx.req): chargeLimiters =>
                    env.security.pwned
                      .isPwned(loginData.password)
                      .flatMap: pwned =>
                        if pwned.yes then chargeLimiters()
                        val isEmail = EmailAddress.isValid(loginData.username.value)
                        api.loadLoginForm(loginData.username, pwned).flatMap {
                          _.bindFromRequest()
                            .fold(
                              err =>
                                chargeLimiters()
                                lila.mon.security.login
                                  .attempt(isEmail, pwned = pwned.yes, result = false)
                                  .increment()
                                negotiate(
                                  lila.security.LoginCandidate.totpError(err) match
                                    case None =>
                                      t3Counter(_.login.failure("credentials"))
                                      Unauthorized.page(loginPage(err, isRemember))
                                    case Some(err) =>
                                      for cookie <- env.security.turnstileCookie.create(loginData)
                                      yield Ok(err).withCookies(cookie),
                                  Unauthorized(doubleJsonFormErrorBody(err))
                                )
                              ,
                              _.toOption match
                                case None => InternalServerError("Authentication error")
                                case Some(u) if u.enabled.no =>
                                  t3Counter(_.login.failure("closed"))
                                  negotiate(
                                    env.mod.logApi.closedByTeacher(u).flatMap {
                                      if _ then
                                        authenticateAppealUser(u, redirectTo, routes.Appeal.closedByTeacher)
                                      else
                                        env.mod.logApi.closedByMod(u).flatMap {
                                          if _ then authenticateAppealUser(u, redirectTo)
                                          else redirectTo(routes.Account.reopen.url)
                                        }
                                    },
                                    Unauthorized(jsonError("This account is closed."))
                                  )
                                case Some(u) =>
                                  lila.mon.security.login
                                    .attempt(isEmail, pwned = pwned.yes, result = true)
                                    .increment()
                                  t3Counter(_.login.success)
                                  env.user.repo.email(u.id).foreach(_.foreach(garbageCollect(u)))
                                  val ref = referrerOr(routes.Lobby.home)
                                  authenticateUser(u, pwned, isRemember, redirectTo(ref).some)
                            )
                        }
                else
                  t3Counter(_.login.failure("turnstile"))
                  BadRequest.page:
                    loginPage(
                      api.loginForm.fill(loginData).withGlobalError("Session timed out, please try again"),
                      isRemember
                    )
          )

  private def t3Counter(counter: lila.mon.signedClient.type => String => kamon.metric.Counter)(using
      Option[ValidReferrer]
  ) = simpleSignup.foreach: ss =>
    counter(lila.mon.signedClient)(ss.client.clientId.value).increment()

  private val clasLoginRateLimit =
    env.security.ipTrust.rateLimit(300, 1.hour, "clas.login")

  def clasLogin = OpenBody:
    Firewall:
      val failRedir = Redirect(routes.Clas.index).flashFailure("Invalid or expired login code")
      bindForm(lila.clas.ClasForm.login)(
        _ => failRedir,
        code =>
          clasLoginRateLimit(rateLimited):
            for
              found <- env.clas.login.login(code)
              res <- found.fold(failRedir.toFuccess): (user, clsId) =>
                val redir = Redirect(routes.Clas.show(clsId)).flashSuccess:
                  lila.core.i18n.I18nKey.emails.welcome_subject.txt(user.username)
                authenticateUser(user, IsPwned.No, false, redir.some)
            yield res
      )

  def logout = Open:
    val sid = env.security.api.reqSessionId(ctx.req)
    for
      _ <- sid.so(env.security.store.delete)
      _ <- sid.so(env.push.browserSub.unsubscribeBySession)
      res <- negotiate(Redirect(routes.Auth.login), jsonOkResult)
    yield res.withCookies(env.security.lilaCookie.newSession)

  // mobile app BC logout with GET
  def logoutGet = Auth { ctx ?=> _ ?=>
    negotiate(
      html = Ok.page(views.auth.logout),
      json = ctx.req.session.get(api.sessionIdKey).map(SessionId.apply).so(env.security.store.delete) >>
        jsonOkResult.withCookies(env.security.lilaCookie.newSession)
    )
  }

  def signup = Open(serveSignup)
  def signupTakex3 = Open(serveSignup(takex3CustomUi, routes.Auth.signupTakex3))
  def signupLang = LangPage(routes.Auth.signup)(serveSignup)

  private def serveSignup(using Context, Option[ValidReferrer]): Fu[Result] =
    serveSignup(none, routes.Auth.signup)

  private def serveSignup(
      customUi: Option[AuthCustomUi],
      canonical: Call
  )(using Context, Option[ValidReferrer]) = NoTor:
    val signedSignup = simpleSignup
    if missingTakex3SimpleSignup(customUi, signedSignup)
    then Redirect(routes.Auth.signup).toFuccess
    else
      t3Counter(_.signup.load)
      val form = forms.signup.full(signedSignup)
      given Option[AuthCustomUi] = customUiOrDefault(customUi)
      val page =
        if isTakex3(customUi) then views.auth.signupTakex3(form.form)
        else views.auth.signup(form.form, form.simple)
      Ok.page(page).map(_.withCanonical(canonical))

  private def simpleSignup(using ref: Option[ValidReferrer]) =
    ref.flatMap(env.oAuth.signedClients.simpleSignupFrom)

  private def missingTakex3SimpleSignup(customUi: Option[AuthCustomUi], signedSignup: Option[SimpleSignup]) =
    isTakex3(customUi) && !signedSignup.exists(_.client == env.oAuth.signedClients.takex3)

  private def authLog(user: UserName, email: Option[EmailAddress], msg: String)(using ctx: Context) =
    for proxy <- env.security.ip2proxy.ofReq(ctx.req)
    do logger.info(s"$proxy $user ${email.fold("-")(_.value)} $msg")

  def signupPost = OpenBody:
    serveSignupPost()

  def signupPostTakex3 = OpenBody:
    serveSignupPost(takex3CustomUi)

  private def serveSignupPost(customUi: Option[AuthCustomUi] = None)(using BodyContext[?]) =
    given Option[AuthCustomUi] = customUiOrDefault(customUi)
    def signupPage(form: Form[?], simple: Boolean) =
      if isTakex3(customUi) then views.auth.signupTakex3(form)
      else views.auth.signup(form, simple)
    NoTor:
      Firewall:
        WithProxy: _ ?=>
          val signedSignup = simpleSignup
          if missingTakex3SimpleSignup(customUi, signedSignup)
          then Redirect(routes.Auth.signup)
          else if HTTPRequest.isLichobile(ctx.req)
          then
            BadRequest:
              jsonError:
                Json.obj("username" -> List("Please use our new mobile app! https://lichess.org/app"))
          else
            limit.enumeration.signup(rateLimited):
              import Signup.Result.*
              env.security.signup
                .website(ctx.blind, signedSignup)
                .flatMap:
                  case RateLimited | ForbiddenNetwork | SimpleSignupDuplicate =>
                    t3Counter(_.signup.failure("rateLimit"))
                    rateLimited
                  case TurnstileFail =>
                    t3Counter(_.signup.failure("turnstile"))
                    val f = forms.signup.full(signedSignup)
                    val form = f.form.withGlobalError("Invalid captcha")
                    BadRequest.page(signupPage(form, f.simple))
                  case FormInvalid(err) =>
                    t3Counter(_.signup.failure("form"))
                    val f = forms.signup.full(signedSignup)
                    BadRequest.page(signupPage(err, f.simple))
                  case ConfirmEmail(user, email) =>
                    t3Counter(_.signup.step("emailConfirm"))
                    redirectWithReferrer(routes.Auth.checkYourEmail).withCookies:
                      EmailConfirm.cookie.newSession(env.security.lilaCookie, user, email)
                  case AllSet(user, email) =>
                    t3Counter(_.signup.success)
                    welcome(user, email, sendWelcomeEmail = true) >> redirectNewUser(user)

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
      EmailConfirm.cookie.get(ctx.req) match
        case None => Ok.async(accountC.renderCheckYourEmail)
        case Some(userEmail) =>
          env.user.repo
            .exists(userEmail.username)
            .flatMap:
              if _ then Ok.async(accountC.renderCheckYourEmail)
              else Redirect(routes.Auth.signup).withCookies(env.security.lilaCookie.newSession)

  // after signup and before confirmation
  def fixEmail = OpenBody:
    EmailConfirm.cookie.get(ctx.req).so { userEmail =>
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
                          for
                            _ <- env.user.repo.setEmail(user.id, newUserEmail.email)
                            _ <- env.security.emailConfirm.send(user, newUserEmail.email)
                          yield redirectWithReferrer(routes.Auth.checkYourEmail).withCookies:
                            EmailConfirm.cookie.newSession(env.security.lilaCookie, user, newUserEmail.email)
                      else Redirect(routes.Auth.login)
        )
    }

  private def redirectWithReferrer(call: Call)(using referrer: Option[ValidReferrer]) =
    Redirect(call.url, referrer.so(r => Map("referrer" -> List(r.value))))

  def signupConfirmEmail(token: String) = Open:
    val ref = summon[Option[ValidReferrer]]
    val result =
      if ref.exists(env.oAuth.signedClients.isSignedReferrer) then
        t3Counter(_.signup.success)
        env.security.emailConfirm.confirm(token)
      else env.security.emailConfirm.dryTest(token)
    result.flatMap(emailConfirmResult(token))

  def signupConfirmEmailPost(token: String) = Open:
    env.security.emailConfirm.confirm(token).flatMap(emailConfirmResult(token))

  private def emailConfirmResult(
      token: String
  )(using ctx: Context): EmailConfirm.Result => Fu[Result] =
    case EmailConfirm.Result.NotFound =>
      lila.mon.user.register.confirmEmailResult(false).increment()
      notFound
    case EmailConfirm.Result.NeedsConfirm(user) => Ok.page(views.auth.signupConfirm(user, token))
    case EmailConfirm.Result.AlreadyConfirmed(user) =>
      if ctx.is(user) then Redirect(routes.User.show(user.username))
      else Redirect(routes.Auth.login)
    case EmailConfirm.Result.JustConfirmed(user) =>
      lila.mon.user.register.confirmEmailResult(true).increment()
      for
        email <- env.user.repo.email(user.id)
        _ <- email.so: email =>
          authLog(user.username, email.some, "Confirmed email")
          welcome(user, email, sendWelcomeEmail = false)
        res <- redirectNewUser(user)
      yield res

  private def redirectNewUser(user: UserModel)(using Context) =
    api
      .saveAuthentication(user.id, ctx.mobileApiVersion, pwned = IsPwned.No)
      .flatMap: sessionId =>
        authenticateCookie(sessionId, remember = true):
          Redirect(referrerOr(routes.User.show(user.username)))
            .flashSuccess("Welcome! Your account is now active.")
      .recoverWith(authRecovery)

  def setFingerPrint(fp: String, ms: Int) = Auth { ctx ?=> me ?=>
    lila.mon.http.fingerPrint.record(ms)
    api
      .setFingerPrint(ctx.req, FingerPrint(fp))
      .logFailure(logger, _ => s"FP ${HTTPRequest.print(ctx.req)} $fp")
      .flatMapz { hash =>
        (!me.lame).so(for
          otherIds <- api.recentUserIdsByFingerHash(hash).map(_.filterNot(_.is(me)))
          _ <- (otherIds.sizeIs >= 2).so(env.user.repo.countLameOrTroll(otherIds).flatMap {
            case nb if nb >= 2 && nb >= otherIds.size / 2 => env.report.api.autoAltPrintReport(me)
            case _ => funit
          })
        yield ())
      }
      .inject(NoContent)
  }

  private def isTakex3(customUi: Option[AuthCustomUi]) = customUi.exists(_.cssClass == "takex3")

  private def renderPasswordReset(
      form: Option[Form[PasswordReset]],
      fail: Option[String],
      customUi: Option[AuthCustomUi] = None
  )(using ctx: Context) =
    given Option[AuthCustomUi] = customUiOrDefault(customUi)
    renderAsync:
      if isTakex3(customUi) then views.auth.passwordResetTakex3(form | env.security.forms.passwordReset, fail)
      else views.auth.passwordReset(form | env.security.forms.passwordReset, fail)

  def passwordReset = Open:
    renderPasswordReset(none, fail = none).map { Ok(_) }

  def passwordResetTakex3 = Open:
    renderPasswordReset(none, fail = none, takex3CustomUi).map { Ok(_) }

  def passwordResetApply = OpenBody:
    servePasswordResetApply(none)

  def passwordResetApplyTakex3 = OpenBody:
    servePasswordResetApply(takex3CustomUi)

  private def servePasswordResetApply(customUi: Option[AuthCustomUi])(using BodyContext[?]) =
    def badRequest(msg: String) = renderPasswordReset(none, fail = msg.some, customUi).map(BadRequest(_))
    env.security.turnstile
      .verify()
      .flatMap:
        if _ then
          forms.passwordReset
            .bindFromRequest()
            .fold(
              err => renderPasswordReset(err.some, fail = "".some, customUi).map { BadRequest(_) },
              data =>
                env.security.passwordReset
                  .limiter(data.email -> req.ipAddress, badRequest("Too many requests")):
                    env.user.repo.notClosedForeverWithEmail(data.email.normalize).flatMap {
                      case Some(user, storedEmail) =>
                        lila.mon.user.auth.passwordResetRequest("success").increment()
                        for _ <- env.security.passwordReset.send(
                            user,
                            storedEmail,
                            source = isTakex3(customUi).option(takex3PasswordResetSource)
                          )
                        yield Redirect(passwordResetSentRoute(storedEmail.value, customUi))
                      case _ =>
                        lila.mon.user.auth.passwordResetRequest("noEmail").increment()
                        Redirect(passwordResetSentRoute(data.email.value, customUi))
                    }
            )
        else badRequest("Invalid captcha")

  private def passwordResetSentRoute(email: String, customUi: Option[AuthCustomUi]) =
    if isTakex3(customUi) then routes.Auth.passwordResetSentTakex3(email)
    else routes.Auth.passwordResetSent(email)

  def passwordResetSent(email: String) = Open:
    Ok.page(views.auth.passwordResetSent(email))

  def passwordResetSentTakex3(email: String) = Open:
    given Option[AuthCustomUi] = takex3CustomUi
    Ok.page(views.auth.passwordResetSentTakex3(email))

  def passwordResetConfirm(token: String) = Open:
    val source = passwordResetSource
    given Option[AuthCustomUi] = passwordResetCustomUi
    env.security.passwordReset
      .confirm(token)
      .flatMap:
        case None =>
          lila.mon.user.auth.passwordResetConfirm("tokenFail").increment()
          notFound
        case Some(user) if user.enabled.no => authenticateAppealUser(user, Redirect(_))
        case Some(user) =>
          given Me = Me(user)
          authLog(user.username, none, "Reset password")
          lila.mon.user.auth.passwordResetConfirm("tokenOk").increment()
          Ok.page:
            if source.isDefined then
              views.auth.passwordResetConfirmTakex3(token, forms.passwdResetForMe, source)
            else views.auth.passwordResetConfirm(token, forms.passwdResetForMe, none)

  def passwordResetConfirmApply(token: String) = OpenBody:
    val source = passwordResetSource
    val customUi = passwordResetCustomUi
    given Option[AuthCustomUi] = customUi
    env.security.passwordReset
      .confirm(token)
      .flatMap:
        case None =>
          lila.mon.user.auth.passwordResetConfirm("tokenPostFail").increment()
          notFound
        case Some(user) if user.enabled.no => authenticateAppealUser(user, Redirect(_))
        case Some(user) =>
          given Me = Me(user)
          FormFuResult(forms.passwdResetForMe) { err =>
            renderPage:
              if source.isDefined then views.auth.passwordResetConfirmTakex3(token, err, source)
              else views.auth.passwordResetConfirm(token, err, false.some)
          } { data =>
            HasherRateLimit:
              for
                _ <- env.security.authenticator.setPassword(user.id, ClearPassword(data.newPasswd1))
                _ <- env.mod.logApi.setPassword
                confirmed <- env.user.repo.setEmailConfirmed(user.id)
                _ <- confirmed.so:
                  welcome(user, _, sendWelcomeEmail = false)
                _ <- env.user.repo.disableTwoFactor(user.id)
                _ <- env.security.store.closeAllSessionsOf(user.id)
                _ <- env.push.browserSub.unsubscribeByUser(user)
                _ <- env.push.unregisterDevices(user)
                result <-
                  if isTakex3(customUi) then
                    renderPage(views.auth.passwordResetSuccessTakex3).map(page => Ok(page).some)
                  else fuccess(none)
                res <- authenticateUser(user, remember = true, pwned = IsPwned.No, result = result)
              yield
                lila.mon.user.auth.passwordResetConfirm("success").increment()
                res
          }

  private def renderMagicLink(form: Option[Form[MagicLink]], fail: Boolean)(using
      Context,
      Option[ValidReferrer]
  ) =
    views.auth.magicLink(form | env.security.forms.magicLink, fail)

  def magicLink = Open:
    Firewall:
      Ok.async(renderMagicLink(none, fail = false))

  def magicLinkApply = OpenBody:
    Firewall:
      env.security.turnstile.verify().flatMap {
        if _ then
          forms.magicLink
            .bindFromRequest()
            .fold(
              err => BadRequest.async(renderMagicLink(err.some, fail = true)),
              data =>
                env.user.repo.notClosedForeverWithEmail(data.email.normalize).flatMap {
                  case Some(user, storedEmail) =>
                    env.security.loginToken.rateLimit[Result](user, storedEmail, ctx.req, rateLimited):
                      for _ <- env.security.loginToken.send(user, storedEmail)
                      yield Redirect(routes.Auth.magicLinkSent)
                  case _ => Redirect(routes.Auth.magicLinkSent)
                }
            )
        else BadRequest.async(renderMagicLink(none, fail = true))
      }

  def magicLinkSent = Open:
    Ok.page(views.auth.magicLinkSent)

  def makeLoginToken = Auth { ctx ?=> me ?=>
    JsonOk:
      env.security.loginToken
        .generate(me)
        .map: token =>
          Json.obj(
            "userId" -> me.userId,
            "url" -> routeUrl(routes.Auth.loginWithToken(token))
          )
  }

  def loginWithToken(token: String) = Open:
    if ctx.isAuth then Redirect(referrerOr(routes.Lobby.home))
    else
      Firewall:
        consumingToken(token): user =>
          Ok.async:
            env.security.loginToken
              .generate(user)
              .map(views.auth.tokenLoginConfirmation(user, _))

  def loginWithTokenPost(token: String) =
    Open:
      if ctx.isAuth then Redirect(referrerOr(routes.Lobby.home))
      else
        Firewall:
          consumingToken(token): user =>
            if user.enabled.yes then authenticateUser(user, remember = true, pwned = IsPwned.No)
            else authenticateAppealUser(user, Redirect(_))

  def check = OpenOrScoped() { ctx ?=>
    ctx.me match
      case Some(me) =>
        val tier =
          if me.is(UserId.lichess) then 4
          else if me.isVerified then 2
          else 1
        NoContent.withHeaders(
          "X-User" -> me.userId.value,
          "X-Tier" -> tier.toString
        )
      case None => Unauthorized
  }

  def apiEmailValidate = ScopedBody() { _ ?=> me ?=>
    if me.isnt(UserId.t3) then notFound
    else bindForm(env.security.forms.signup.emailCheck)(jsonFormError, JsonOk(_))
  }

  private def consumingToken(token: String)(f: UserModel => Fu[Result])(using Context) =
    env.security.loginToken
      .consume(token)
      .flatMap:
        case None =>
          BadRequest.page:
            import scalatags.Text.all.stringFrag
            views.site.message("This token has expired.")(stringFrag("Please go back and try again."))
        case Some(user) => f(user)

  private[controllers] object LoginRateLimit:
    def apply(id: UserIdOrEmail, req: RequestHeader)(run: RateLimit.Charge => Fu[Result])(using
        Context
    ): Fu[Result] =
      passwordCost(req).flatMap: cost =>
        env.security.passwordHasher.rateLimit[Result](
          rateLimited,
          enforce = env.net.rateLimit,
          ipCost = cost.toInt
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
      .rateLimitCostFactor(req, _.proxyMultiplier(if HTTPRequest.nginxWhitelist(req) then 1 else 2))

  private[controllers] def EmailConfirmRateLimit = EmailConfirm.rateLimit[Result]

  private[controllers] def RedirectToProfileIfLoggedIn(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.me.fold(f)(me => Redirect(routes.User.show(me.username)))
