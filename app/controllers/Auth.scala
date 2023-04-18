package controllers

import alleycats.Zero
import play.api.data.FormError
import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.api.Context
import lila.app.{ given, * }
import lila.common.{ EmailAddress, HTTPRequest }
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

  private def mobileUserOk(u: UserModel, sessionId: String)(implicit ctx: Context): Fu[Result] =
    env.round.proxyRepo urgentGames u map { povs =>
      Ok {
        env.user.jsonView.full(
          u,
          withRating = ctx.pref.showRatings,
          withProfile = true
        ) ++ Json.obj(
          "nowPlaying" -> JsArray(povs take 20 map env.api.lobbyApi.nowPlaying),
          "sessionId"  -> sessionId
        )
      }
    }

  private def getReferrerOption(implicit ctx: Context): Option[String] =
    get("referrer").flatMap(env.api.referrerRedirect.valid) orElse ctxReq.session.get(
      api.AccessUri
    )

  private def getReferrer(implicit ctx: Context): String = getReferrerOption | routes.Lobby.home.url

  def authenticateUser(u: UserModel, remember: Boolean, result: Option[String => Result] = None)(using
      ctx: Context
  ): Fu[Result] =
    api.saveAuthentication(u.id, ctx.mobileApiVersion) flatMap { sessionId =>
      negotiate(
        html = fuccess {
          result.fold(Redirect(getReferrer))(_(getReferrer))
        },
        api = _ => mobileUserOk(u, sessionId)
      ) map authenticateCookie(sessionId, remember)
    } recoverWith authRecovery

  private def authenticateAppealUser(u: UserModel, redirect: String => Result)(using
      ctx: Context
  ): Fu[Result] =
    api.appeal.saveAuthentication(u.id) flatMap { sessionId =>
      negotiate(
        html = redirect(appeal.routes.Appeal.landing.url).toFuccess map
          authenticateCookie(sessionId, remember = false),
        api = _ => NotFound.toFuccess
      )
    } recoverWith authRecovery

  private def authenticateCookie(sessionId: String, remember: Boolean)(
      result: Result
  )(using req: RequestHeader) =
    result.withCookies(
      env.lilaCookie.withSession(remember = remember) {
        _ + (api.sessionIdKey -> sessionId) - api.AccessUri - lila.security.EmailConfirm.cookie.name
      }
    )

  private def authRecovery(using ctx: Context): PartialFunction[Throwable, Fu[Result]] =
    case lila.security.SecurityApi.MustConfirmEmail(_) =>
      fuccess {
        if (HTTPRequest isXhr ctx.req) Ok(s"ok:${routes.Auth.checkYourEmail}")
        else BadRequest(accountC.renderCheckYourEmail)
      }

  def login     = Open(serveLogin(using _))
  def loginLang = LangPage(routes.Auth.login)(serveLogin(using _))

  private def serveLogin(using ctx: Context) = NoBot {
    val referrer = get("referrer") flatMap env.api.referrerRedirect.valid
    val switch   = get("switch")
    referrer ifTrue ctx.isAuth ifTrue switch.isEmpty match
      case Some(url) => Redirect(url).toFuccess // redirect immediately if already logged in
      case None =>
        val prefillUsername = lila.security.UserStrOrEmail(~switch.filter(_ != "1"))
        val form            = api.loginFormFilled(prefillUsername)
        Ok(html.auth.login(form, referrer)).withCanonical(routes.Auth.login).toFuccess
  }

  private val is2fa = Set("MissingTotpToken", "InvalidTotpToken")

  def authenticate = OpenBody { implicit ctx =>
    NoCrawlers {
      Firewall {
        def redirectTo(url: String)   = if (HTTPRequest isXhr ctx.req) Ok(s"ok:$url") else Redirect(url)
        given play.api.mvc.Request[?] = ctx.body
        val referrer = get("referrer").filterNot(env.api.referrerRedirect.sillyLoginReferrers)
        api.loginForm
          .bindFromRequest()
          .fold(
            err =>
              negotiate(
                html = Unauthorized(html.auth.login(err, referrer)).toFuccess,
                api = _ => Unauthorized(ridiculousBackwardCompatibleJsonError(errorsAsJson(err))).toFuccess
              ),
            (login, pass) =>
              LoginRateLimit(login.normalize, ctx.req) { chargeLimiters =>
                env.security.pwned(pass) foreach { _ ?? chargeLimiters() }
                val isEmail  = EmailAddress.isValid(login.value)
                val stuffing = ctx.req.headers.get("X-Stuffing") | "no" // from nginx
                api.loadLoginForm(login) flatMap {
                  _.bindFromRequest()
                    .fold(
                      err => {
                        chargeLimiters()
                        lila.mon.security.login
                          .attempt(isEmail, stuffing = stuffing, result = false)
                          .increment()
                        negotiate(
                          html = fuccess {
                            err.errors match
                              case List(FormError("", Seq(err), _)) if is2fa(err) => Ok(err)
                              case _ => Unauthorized(html.auth.login(err, referrer))
                          },
                          api = _ =>
                            Unauthorized(
                              ridiculousBackwardCompatibleJsonError(errorsAsJson(err))
                            ).toFuccess
                        )
                      },
                      result =>
                        result.toOption match {
                          case None => InternalServerError("Authentication error").toFuccess
                          case Some(u) if u.enabled.no =>
                            negotiate(
                              html = env.mod.logApi.closedByMod(u) flatMap {
                                case true => authenticateAppealUser(u, redirectTo)
                                case _    => redirectTo(routes.Account.reopen.url).toFuccess
                              },
                              api = _ => Unauthorized(jsonError("This account is closed.")).toFuccess
                            )
                          case Some(u) =>
                            lila.mon.security.login.attempt(isEmail, stuffing = stuffing, result = true)
                            env.user.repo.email(u.id) foreach { _ foreach garbageCollect(u) }
                            val remember = api.rememberForm.bindFromRequest().value | true
                            authenticateUser(u, remember, Some(redirectTo))
                        }
                    )
                }
              }
          )
      }
    }
  }

  def logout =
    Open { implicit ctx =>
      val currentSessionId = ~env.security.api.reqSessionId(ctx.req)
      env.security.store.delete(currentSessionId) >>
        env.push.webSubscriptionApi.unsubscribeBySession(currentSessionId) >>
        negotiate(
          html = Redirect(routes.Auth.login).toFuccess,
          api = _ => Ok(Json.obj("ok" -> true)).toFuccess
        ).dmap(_.withCookies(env.lilaCookie.newSession))
    }

  // mobile app BC logout with GET
  def logoutGet =
    Auth { implicit ctx => _ =>
      negotiate(
        html = Ok(html.auth.bits.logout()).toFuccess,
        api = _ => {
          ctxReq.session get api.sessionIdKey foreach env.security.store.delete
          Ok(Json.obj("ok" -> true)).withCookies(env.lilaCookie.newSession).toFuccess
        }
      )
    }

  def signup     = Open(serveSignup(_))
  def signupLang = LangPage(routes.Auth.signup)(serveSignup(_))
  private def serveSignup(implicit ctx: Context) =
    NoTor {
      forms.signup.website map { form =>
        Ok(html.auth.signup(form))
      }
    }

  private def authLog(user: String, email: String, msg: String) =
    lila.log("auth").info(s"$user $email $msg")

  def signupPost =
    OpenBody { implicit ctx =>
      given play.api.mvc.Request[?] = ctx.body
      NoTor {
        Firewall {
          forms.preloadEmailDns() >> negotiate(
            html = env.security.signup
              .website(ctx.blind)
              .flatMap {
                case Signup.Result.RateLimited => limitedDefault.zero.toFuccess
                case Signup.Result.MissingCaptcha =>
                  forms.signup.website map { form =>
                    BadRequest(html.auth.signup(form))
                  }
                case Signup.Result.Bad(err) =>
                  forms.signup.website map { baseForm =>
                    BadRequest(html.auth.signup(baseForm withForm err))
                  }
                case Signup.Result.ConfirmEmail(user, email) =>
                  fuccess {
                    Redirect(routes.Auth.checkYourEmail) withCookies
                      lila.security.EmailConfirm.cookie
                        .make(env.lilaCookie, user, email)(using ctx.req)
                  }
                case Signup.Result.AllSet(user, email) =>
                  welcome(user, email, sendWelcomeEmail = true) >> redirectNewUser(user)
              },
            api = apiVersion =>
              env.security.signup
                .mobile(apiVersion)
                .flatMap {
                  case Signup.Result.RateLimited        => limitedDefault.zero.toFuccess
                  case Signup.Result.MissingCaptcha     => fuccess(BadRequest(jsonError("Missing captcha?!")))
                  case Signup.Result.Bad(err)           => jsonFormError(err)
                  case Signup.Result.ConfirmEmail(_, _) => Ok(Json.obj("email_confirm" -> true)).toFuccess
                  case Signup.Result.AllSet(user, email) =>
                    welcome(user, email, sendWelcomeEmail = true) >> authenticateUser(user, remember = true)
                }
          )
        }
      }
    }

  private def welcome(user: UserModel, email: EmailAddress, sendWelcomeEmail: Boolean)(using
      ctx: Context
  ): Funit =
    garbageCollect(user)(email)
    if (sendWelcomeEmail) env.mailer.automaticEmail.welcomeEmail(user, email)
    env.mailer.automaticEmail.welcomePM(user)
    env.pref.api.saveNewUserPrefs(user, ctx.req)

  private def garbageCollect(user: UserModel)(email: EmailAddress)(using ctx: Context) =
    env.security.garbageCollector.delay(user, email, ctx.req)

  def checkYourEmail =
    Open { implicit ctx =>
      RedirectToProfileIfLoggedIn {
        lila.security.EmailConfirm.cookie get ctx.req match
          case None => Ok(accountC.renderCheckYourEmail).toFuccess
          case Some(userEmail) =>
            env.user.repo exists userEmail.username map {
              case false => Redirect(routes.Auth.signup) withCookies env.lilaCookie.newSession(using ctx.req)
              case true  => Ok(accountC.renderCheckYourEmail)
            }
      }
    }

  // after signup and before confirmation
  def fixEmail =
    OpenBody { implicit ctx =>
      lila.security.EmailConfirm.cookie.get(ctx.req) ?? { userEmail =>
        given play.api.mvc.Request[?] = ctx.body
        forms.preloadEmailDns() >> forms
          .fixEmail(userEmail.email)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.auth.checkYourEmail(userEmail.some, err.some)).toFuccess,
            email =>
              env.user.repo.byId(userEmail.username) flatMap {
                _.fold(Redirect(routes.Auth.signup).toFuccess) { user =>
                  env.user.repo.mustConfirmEmail(user.id) flatMap {
                    case false => Redirect(routes.Auth.login).toFuccess
                    case _ =>
                      val newUserEmail = userEmail.copy(email = email)
                      EmailConfirmRateLimit(newUserEmail, ctx.req) {
                        lila.mon.email.send.fix.increment()
                        env.user.repo.setEmail(user.id, newUserEmail.email) >>
                          env.security.emailConfirm.send(user, newUserEmail.email) inject {
                            Redirect(routes.Auth.checkYourEmail) withCookies
                              lila.security.EmailConfirm.cookie
                                .make(env.lilaCookie, user, newUserEmail.email)(using ctx.req)
                          }
                      }(rateLimitedFu)
                  }
                }
              }
          )
      }
    }

  def signupConfirmEmail(token: String) =
    Open { implicit ctx =>
      import lila.security.EmailConfirm.Result
      env.security.emailConfirm.confirm(token) flatMap {
        case Result.NotFound =>
          lila.mon.user.register.confirmEmailResult(false).increment()
          notFound
        case Result.AlreadyConfirmed(user) if ctx.is(user) =>
          Redirect(routes.User.show(user.username)).toFuccess
        case Result.AlreadyConfirmed(_) =>
          Redirect(routes.Auth.login).toFuccess
        case Result.JustConfirmed(user) =>
          lila.mon.user.register.confirmEmailResult(true).increment()
          env.user.repo.email(user.id).flatMap {
            _.?? { email =>
              authLog(user.username, email.value, s"Confirmed email ${email.value}")
              welcome(user, email, sendWelcomeEmail = false)
            }
          } >> redirectNewUser(user)
      }
    }

  private def redirectNewUser(user: UserModel)(implicit ctx: Context) =
    api.saveAuthentication(user.id, ctx.mobileApiVersion) flatMap { sessionId =>
      negotiate(
        html = Redirect(getReferrerOption | routes.User.show(user.username).url).toFuccess,
        api = _ => mobileUserOk(user, sessionId)
      ) map authenticateCookie(sessionId, remember = true)
    } recoverWith authRecovery

  def setFingerPrint(fp: String, ms: Int) =
    Auth { ctx => me =>
      lila.mon.http.fingerPrint.record(ms)
      api
        .setFingerPrint(ctx.req, FingerPrint(fp))
        .logFailure(lila log "fp", _ => s"${HTTPRequest print ctx.req} $fp") flatMapz { hash =>
        !me.lame ?? (for
          otherIds <- api.recentUserIdsByFingerHash(hash).map(_.filter(me.id.!=))
          _ <- (otherIds.sizeIs >= 2) ?? env.user.repo.countLameOrTroll(otherIds).flatMap {
            case nb if nb >= 2 && nb >= otherIds.size / 2 => env.report.api.autoAltPrintReport(me.id)
            case _                                        => funit
          }
        yield ())
      } inject NoContent
    }

  private def renderPasswordReset(form: Option[play.api.data.Form[PasswordReset]], fail: Boolean)(using
      ctx: Context
  ) =
    env.security.forms.passwordReset map { baseForm =>
      html.auth.bits.passwordReset(form.foldLeft(baseForm)(_ withForm _), fail)
    }

  def passwordReset =
    Open { implicit ctx =>
      renderPasswordReset(none, fail = false) map { Ok(_) }
    }

  def passwordResetApply =
    OpenBody { implicit ctx =>
      given play.api.mvc.Request[?] = ctx.body
      env.security.hcaptcha.verify() flatMap { captcha =>
        if (captcha.ok)
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
                      Redirect(routes.Auth.passwordResetSent(data.email.conceal)).toFuccess
                  }
              )
          }
        else renderPasswordReset(none, fail = true) map { BadRequest(_) }
      }
    }

  def passwordResetSent(email: String) =
    Open { implicit ctx =>
      fuccess {
        Ok(html.auth.bits.passwordResetSent(email))
      }
    }

  def passwordResetConfirm(token: String) =
    Open { implicit ctx =>
      env.security.passwordReset confirm token flatMap {
        case None =>
          lila.mon.user.auth.passwordResetConfirm("tokenFail").increment()
          notFound
        case Some(user) =>
          authLog(user.username, "-", "Reset password")
          lila.mon.user.auth.passwordResetConfirm("tokenOk").increment()
          fuccess(html.auth.bits.passwordResetConfirm(user, token, forms.passwdResetFor(user), none))
      }
    }

  def passwordResetConfirmApply(token: String) =
    OpenBody { implicit ctx =>
      env.security.passwordReset confirm token flatMap {
        case None =>
          lila.mon.user.auth.passwordResetConfirm("tokenPostFail").increment()
          notFound
        case Some(user) =>
          given play.api.mvc.Request[?] = ctx.body
          FormFuResult(forms.passwdResetFor(user)) { err =>
            fuccess(html.auth.bits.passwordResetConfirm(user, token, err, false.some))
          } { data =>
            HasherRateLimit(user.id, ctx.req) {
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
      }
    }

  private def renderMagicLink(form: Option[play.api.data.Form[MagicLink]], fail: Boolean)(using
      ctx: Context
  ) =
    env.security.forms.magicLink map { baseForm =>
      html.auth.bits.magicLink(form.foldLeft(baseForm)(_ withForm _), fail)
    }

  def magicLink =
    Open { implicit ctx =>
      Firewall {
        renderMagicLink(none, fail = false) map { Ok(_) }
      }
    }

  def magicLinkApply =
    OpenBody { implicit ctx =>
      Firewall {
        given play.api.mvc.Request[?] = ctx.body
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
                        MagicLinkRateLimit(user, storedEmail, ctx.req) {
                          lila.mon.user.auth.magicLinkRequest("success").increment()
                          env.security.magicLink.send(user, storedEmail) inject Redirect(
                            routes.Auth.magicLinkSent
                          )
                        }(rateLimitedFu)
                      case _ =>
                        lila.mon.user.auth.magicLinkRequest("no_email").increment()
                        Redirect(routes.Auth.magicLinkSent).toFuccess
                    }
                )
            }
          else
            renderMagicLink(none, fail = true) map { BadRequest(_) }
        }
      }
    }

  def magicLinkSent =
    Open { implicit ctx =>
      fuccess {
        Ok(html.auth.bits.magicLinkSent)
      }
    }

  private lazy val magicLinkLoginRateLimitPerToken = RateLimit[String](
    credits = 3,
    duration = 1 hour,
    key = "login.magicLink.token"
  )

  def magicLinkLogin(token: String) =
    Open { implicit ctx =>
      if (ctx.isAuth) Redirect(routes.Lobby.home).toFuccess
      else
        Firewall {
          magicLinkLoginRateLimitPerToken(token) {
            env.security.magicLink confirm token flatMap {
              case None =>
                lila.mon.user.auth.magicLinkConfirm("token_fail").increment()
                notFound
              case Some(user) =>
                authLog(user.username, "-", "Magic link")
                authenticateUser(user, remember = true) >>-
                  lila.mon.user.auth.magicLinkConfirm("success").increment().unit
            }
          }(rateLimitedFu)
        }
    }

  private def loginTokenFor(me: UserModel) = JsonOk {
    env.security.loginToken generate me map { token =>
      Json.obj(
        "userId" -> me.id,
        "url"    -> s"${env.net.baseUrl}${routes.Auth.loginWithToken(token).url}"
      )
    }
  }

  def makeLoginToken =
    AuthOrScoped(_.Web.Login)(
      _ => loginTokenFor,
      req =>
        user => {
          lila.log("oauth").info(s"api makeLoginToken ${user.id} ${HTTPRequest printClient req}")
          loginTokenFor(user)
        }
    )

  def loginWithToken(token: String) =
    Open { implicit ctx =>
      if (ctx.isAuth) Redirect(getReferrer).toFuccess
      else
        Firewall {
          consumingToken(token) { user =>
            env.security.loginToken.generate(user) map { newToken =>
              Ok(html.auth.bits.tokenLoginConfirmation(user, newToken, get("referrer")))
            }
          }
        }
    }

  def loginWithTokenPost(token: String, @annotation.nowarn referrer: Option[String]) =
    Open { implicit ctx =>
      if (ctx.isAuth) Redirect(getReferrer).toFuccess
      else
        Firewall {
          consumingToken(token) { authenticateUser(_, remember = true) }
        }
    }

  private def consumingToken(token: String)(f: UserModel => Fu[Result])(using Context) =
    env.security.loginToken consume token flatMap {
      case None =>
        BadRequest {
          import scalatags.Text.all.stringFrag
          html.site.message("This token has expired.")(stringFrag("Please go back and try again."))
        }.toFuccess
      case Some(user) => f(user)
    }

  private given limitedDefault: Zero[Result] = Zero(rateLimited)

  private[controllers] def LoginRateLimit(id: UserIdOrEmail, req: RequestHeader)(
      run: RateLimit.Charge => Fu[Result]
  ): Fu[Result] =
    env.security.ipTrust.isSuspicious(req.ipAddress) flatMap { ipSusp =>
      PasswordHasher.rateLimit[Result](
        enforce = env.net.rateLimit,
        ipCost = 1 + ipSusp.??(15) + EmailAddress.isValid(id.value).??(2)
      )(id, req)(run)(rateLimitedFu)
    }

  private[controllers] def HasherRateLimit(id: UserId, req: RequestHeader)(run: => Fu[Result]): Fu[Result] =
    env.security.ip2proxy(req.ipAddress) flatMap { proxy =>
      PasswordHasher.rateLimit[Result](
        enforce = env.net.rateLimit,
        ipCost = if proxy.is then 10 else 1
      )(id into UserIdOrEmail, req)(_ => run)(rateLimitedFu)
    }

  private[controllers] def EmailConfirmRateLimit = lila.security.EmailConfirm.rateLimit[Result]

  private[controllers] def MagicLinkRateLimit = lila.security.MagicLink.rateLimit[Result]

  private[controllers] def RedirectToProfileIfLoggedIn(f: => Fu[Result])(using ctx: Context): Fu[Result] =
    ctx.me match
      case Some(me) => Redirect(routes.User.show(me.username)).toFuccess
      case None     => f
