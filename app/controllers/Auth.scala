package controllers

import ornicar.scalalib.Zero
import play.api.data.{ Form, FormError }
import play.api.libs.json._
import play.api.mvc._

import lila.api.Context
import lila.app._
import lila.common.{ HTTPRequest, EmailAddress }
import lila.security.{ EmailAddressValidator, FingerPrint }
import lila.user.{ User => UserModel, PasswordHasher }
import UserModel.ClearPassword
import views._

final class Auth(
    env: Env,
    accountC: => Account
) extends LilaController(env) {

  private def api = env.security.api
  private def forms = env.security.forms

  private def mobileUserOk(u: UserModel, sessionId: String): Fu[Result] =
    env.round.proxyRepo urgentGames u map { povs =>
      Ok {
        env.user.jsonView(u) ++ Json.obj(
          "nowPlaying" -> JsArray(povs take 20 map env.api.lobbyApi.nowPlaying),
          "sessionId" -> sessionId
        )
      }
    }

  private val refRegex = """[\w@/\-=?]++""".r

  // do not allow redirects to external sites,
  // nor redirect back to /mobile (which is shown after logout)
  private def goodReferrer(referrer: String): Boolean =
    referrer.nonEmpty &&
      referrer.stripPrefix("/") != "mobile" && {
        (!referrer.contains("//") && refRegex.matches(referrer)) ||
          referrer.startsWith(env.net.baseUrl.value)
      }

  def authenticateUser(u: UserModel, result: Option[String => Result] = None)(implicit ctx: Context): Fu[Result] = {
    if (u.ipBan) fuccess(Redirect(routes.Lobby.home))
    else api.saveAuthentication(u.id, ctx.mobileApiVersion) flatMap { sessionId =>
      negotiate(
        html = fuccess {
          val redirectTo = get("referrer").filter(goodReferrer) orElse
            ctxReq.session.get(api.AccessUri) getOrElse
            routes.Lobby.home.url
          result.fold(Redirect(redirectTo))(_(redirectTo))
        },
        api = _ => mobileUserOk(u, sessionId)
      ) map authenticateCookie(sessionId)
    } recoverWith authRecovery
  }

  private def authenticateCookie(sessionId: String)(result: Result)(implicit req: RequestHeader) =
    result.withCookies(
      env.lilaCookie.withSession {
        _ + (api.sessionIdKey -> sessionId) - api.AccessUri - lila.security.EmailConfirm.cookie.name
      }
    )

  private def authRecovery(implicit ctx: Context): PartialFunction[Throwable, Fu[Result]] = {
    case lila.security.SecurityApi.MustConfirmEmail(_) => fuccess {
      if (HTTPRequest isXhr ctx.req) Ok(s"ok:${routes.Auth.checkYourEmail}")
      else BadRequest(accountC.renderCheckYourEmail)
    }
  }

  def login = Open { implicit ctx =>
    val referrer = get("referrer").filter(goodReferrer)
    referrer.filterNot(_ contains "/login") ifTrue ctx.isAuth match {
      case Some(url) => Redirect(url).fuccess // redirect immediately if already logged in
      case None => Ok(html.auth.login(api.loginForm, referrer)).fuccess
    }
  }

  private val is2fa = Set("MissingTotpToken", "InvalidTotpToken")

  def authenticate = OpenBody { implicit ctx =>
    def redirectTo(url: String) = if (HTTPRequest isXhr ctx.req) Ok(s"ok:$url") else Redirect(url)
    Firewall({
      implicit val req = ctx.body
      val referrer = get("referrer")
      api.usernameOrEmailForm.bindFromRequest.fold(
        err => negotiate(
          html = Unauthorized(html.auth.login(api.loginForm, referrer)).fuccess,
          api = _ => Unauthorized(ridiculousBackwardCompatibleJsonError(errorsAsJson(err))).fuccess
        ),
        usernameOrEmail => HasherRateLimit(usernameOrEmail, ctx.req) { chargeIpLimiter =>
          api.loadLoginForm(usernameOrEmail) flatMap { loginForm =>
            loginForm.bindFromRequest.fold(
              err => {
                chargeIpLimiter(1)
                negotiate(
                  html = fuccess {
                    err.errors match {
                      case List(FormError("", List(err), _)) if is2fa(err) => Ok(err)
                      case _ => Unauthorized(html.auth.login(err, referrer))
                    }
                  },
                  api = _ => Unauthorized(ridiculousBackwardCompatibleJsonError(errorsAsJson(err))).fuccess
                )
              },
              result => result.toOption match {
                case None => InternalServerError("Authentication error").fuccess
                case Some(u) =>
                  env.user.repo.email(u.id) foreach {
                    _ foreach { garbageCollect(u, _) }
                  }
                  authenticateUser(u, Some(redirectTo))
              }
            )
          }
        }
      )
    }, redirectTo("/").fuccess)
  }

  def logout = Open { implicit ctx =>
    val currentSessionId = ~env.security.api.reqSessionId(ctx.req)
    env.security.store.delete(currentSessionId) >>
      env.push.webSubscriptionApi.unsubscribeBySession(currentSessionId) >>
      negotiate(
        html = Redirect(routes.Auth.login).fuccess,
        api = _ => Ok(Json.obj("ok" -> true)).fuccess
      ).dmap(_.withCookies(env.lilaCookie.newSession))
  }

  // mobile app BC logout with GET
  def logoutGet = Open { implicit ctx =>
    negotiate(
      html = notFound,
      api = _ => {
        ctxReq.session get api.sessionIdKey foreach env.security.store.delete
        Ok(Json.obj("ok" -> true)).withCookies(env.lilaCookie.newSession).fuccess
      }
    )
  }

  def signup = Open { implicit ctx =>
    NoTor {
      Ok(html.auth.signup(forms.signup.website, env.security.recaptchaPublicConfig)).fuccess
    }
  }

  private sealed abstract class MustConfirmEmail(val value: Boolean)
  private object MustConfirmEmail {

    case object Nope extends MustConfirmEmail(false)
    case object YesBecausePrintExists extends MustConfirmEmail(true)
    case object YesBecausePrintMissing extends MustConfirmEmail(true)
    case object YesBecauseIpExists extends MustConfirmEmail(true)
    case object YesBecauseIpSusp extends MustConfirmEmail(true)
    case object YesBecauseMobile extends MustConfirmEmail(true)
    case object YesBecauseUA extends MustConfirmEmail(true)

    def apply(print: Option[FingerPrint])(implicit ctx: Context): Fu[MustConfirmEmail] = {
      val ip = HTTPRequest lastRemoteAddress ctx.req
      api.recentByIpExists(ip) flatMap { ipExists =>
        if (ipExists) fuccess(YesBecauseIpExists)
        else if (HTTPRequest weirdUA ctx.req) fuccess(YesBecauseUA)
        else print.fold[Fu[MustConfirmEmail]](fuccess(YesBecausePrintMissing)) { fp =>
          api.recentByPrintExists(fp) flatMap { printFound =>
            if (printFound) fuccess(YesBecausePrintExists)
            else env.security.ipTrust.isSuspicious(ip).map {
              case true => YesBecauseIpSusp
              case _ => Nope
            }
          }
        }
      }
    }
  }

  private def authLog(user: String, email: String, msg: String) =
    lila.log("auth").info(s"$user $email $msg")

  private def signupErrLog(err: Form[_])(implicit ctx: Context) = for {
    username <- err("username").value
    email <- err("email").value
  } {
    authLog(username, email, s"Signup fail: ${Json stringify errorsAsJson(err)}")
    if (err.errors.exists(_.messages.contains("error.email_acceptable")) &&
      err("email").value.exists(EmailAddress.matches))
      authLog(username, email, s"Signup with unacceptable email")
  }

  def signupPost = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    NoTor {
      Firewall {
        forms.preloadEmailDns >> negotiate(
          html = forms.signup.website.bindFromRequest.fold(
            err => {
              signupErrLog(err)
              BadRequest(html.auth.signup(err, env.security.recaptchaPublicConfig)).fuccess
            },
            data => env.security.recaptcha.verify(~data.recaptchaResponse, req).flatMap {
              case false =>
                authLog(data.username, data.email, "Signup recaptcha fail")
                BadRequest(html.auth.signup(forms.signup.website fill data, env.security.recaptchaPublicConfig)).fuccess
              case true => HasherRateLimit(data.username, ctx.req) { _ =>
                MustConfirmEmail(data.fingerPrint) flatMap { mustConfirm =>
                  lila.mon.user.register.count(none)
                  lila.mon.user.register.mustConfirmEmail(mustConfirm.toString)()
                  val email = env.security.emailAddressValidator.validate(data.realEmail) err s"Invalid email ${data.email}"
                  val passwordHash = env.user.authenticator passEnc ClearPassword(data.password)
                  env.user.repo.create(data.username, passwordHash, email.acceptable, ctx.blind, none,
                    mustConfirmEmail = mustConfirm.value)
                    .orFail(s"No user could be created for ${data.username}")
                    .addEffect { logSignup(_, email.acceptable, data.fingerPrint, mustConfirm) }
                    .map(_ -> email).flatMap {
                      case (user, EmailAddressValidator.Acceptable(email)) if mustConfirm.value =>
                        env.security.emailConfirm.send(user, email) >> {
                          if (env.security.emailConfirm.effective)
                            api.saveSignup(user.id, ctx.mobileApiVersion, data.fingerPrint) inject {
                              Redirect(routes.Auth.checkYourEmail) withCookies
                                lila.security.EmailConfirm.cookie.make(env.lilaCookie, user, email)(ctx.req)
                            }
                          else welcome(user, email) >> redirectNewUser(user)
                        }
                      case (user, EmailAddressValidator.Acceptable(email)) =>
                        welcome(user, email) >> redirectNewUser(user)
                    }
                }
              }
            }
          ),
          api = apiVersion => forms.signup.mobile.bindFromRequest.fold(
            err => {
              signupErrLog(err)
              jsonFormError(err)
            },
            data => HasherRateLimit(data.username, ctx.req) { _ =>
              val email = env.security.emailAddressValidator.validate(data.realEmail) err s"Invalid email ${data.email}"
              val mustConfirm = MustConfirmEmail.YesBecauseMobile
              lila.mon.user.register.count(apiVersion.some)
              lila.mon.user.register.mustConfirmEmail(mustConfirm.toString)()
              val passwordHash = env.user.authenticator passEnc ClearPassword(data.password)
              env.user.repo.create(data.username, passwordHash, email.acceptable, false, apiVersion.some,
                mustConfirmEmail = mustConfirm.value)
                .orFail(s"No user could be created for ${data.username}")
                .addEffect { logSignup(_, email.acceptable, none, mustConfirm) }
                .map(_ -> email).flatMap {
                  case (user, EmailAddressValidator.Acceptable(email)) if mustConfirm.value =>
                    env.security.emailConfirm.send(user, email) >> {
                      if (env.security.emailConfirm.effective) Ok(Json.obj("email_confirm" -> true)).fuccess
                      else welcome(user, email) >> authenticateUser(user)
                    }
                  case (user, _) => welcome(user, email.acceptable) >> authenticateUser(user)
                }
            }
          )
        )
      }
    }
  }

  private def logSignup(
    user: UserModel,
    email: EmailAddress,
    fingerPrint: Option[lila.security.FingerPrint],
    mustConfirm: MustConfirmEmail
  )(implicit ctx: Context) = {
    authLog(user.username, email.value, s"fp: ${fingerPrint} mustConfirm: $mustConfirm")
    val ip = HTTPRequest lastRemoteAddress ctx.req
    env.security.ipTrust.isSuspicious(ip) foreach { susp =>
      env.slack.api.signup(user, email, ip, fingerPrint.flatMap(_.hash).map(_.value), susp)
    }
  }

  private def welcome(user: UserModel, email: EmailAddress)(implicit ctx: Context): Funit = {
    garbageCollect(user, email)
    env.security.automaticEmail.welcome(user, email)
    env.pref.api.saveNewUserPrefs(user, ctx.req)
  }

  private def garbageCollect(user: UserModel, email: EmailAddress)(implicit ctx: Context) =
    env.security.garbageCollector.delay(user, email, ctx.req)

  def checkYourEmail = Open { implicit ctx =>
    ctx.me match {
      case Some(me) => Redirect(routes.User.show(me.username)).fuccess
      case None => lila.security.EmailConfirm.cookie get ctx.req match {
        case None => Ok(accountC.renderCheckYourEmail).fuccess
        case Some(userEmail) =>
          env.user.repo nameExists userEmail.username map {
            case false => Redirect(routes.Auth.signup) withCookies env.lilaCookie.newSession(ctx.req)
            case true => Ok(accountC.renderCheckYourEmail)
          }
      }
    }
  }

  // after signup and before confirmation
  def fixEmail = OpenBody { implicit ctx =>
    lila.security.EmailConfirm.cookie.get(ctx.req) ?? { userEmail =>
      implicit val req = ctx.body
      forms.preloadEmailDns >> forms.fixEmail(userEmail.email).bindFromRequest.fold(
        err => BadRequest(html.auth.checkYourEmail(userEmail.some, err.some)).fuccess,
        email => env.user.repo.named(userEmail.username) flatMap {
          _.fold(Redirect(routes.Auth.signup).fuccess) { user =>
            env.user.repo.mustConfirmEmail(user.id) flatMap {
              case false => Redirect(routes.Auth.login).fuccess
              case _ =>
                val newUserEmail = userEmail.copy(email = EmailAddress(email))
                EmailConfirmRateLimit(newUserEmail, ctx.req) {
                  lila.mon.email.types.fix()
                  env.user.repo.setEmail(user.id, newUserEmail.email) >>
                    env.security.emailConfirm.send(user, newUserEmail.email) inject {
                      Redirect(routes.Auth.checkYourEmail) withCookies
                        lila.security.EmailConfirm.cookie.make(env.lilaCookie, user, newUserEmail.email)(ctx.req)
                    }
                }
            }
          }
        }
      )
    }
  }

  def signupConfirmEmail(token: String) = Open { implicit ctx =>
    import lila.security.EmailConfirm.Result
    env.security.emailConfirm.confirm(token) flatMap {
      case Result.NotFound =>
        lila.mon.user.register.confirmEmailResult(false)()
        notFound
      case Result.AlreadyConfirmed(user) if ctx.is(user) =>
        Redirect(routes.User.show(user.username)).fuccess
      case Result.AlreadyConfirmed(user) =>
        Redirect(routes.Auth.login).fuccess
      case Result.JustConfirmed(user) =>
        lila.mon.user.register.confirmEmailResult(true)()
        env.user.repo.email(user.id).flatMap {
          _.?? { email =>
            authLog(user.username, email.value, s"Confirmed email ${email.value}")
            welcome(user, email)
          }
        } >> redirectNewUser(user)
    }
  }

  private def redirectNewUser(user: UserModel)(implicit ctx: Context) = {
    api.saveAuthentication(user.id, ctx.mobileApiVersion) flatMap { sessionId =>
      negotiate(
        html = Redirect(routes.User.show(user.username)).fuccess,
        api = _ => mobileUserOk(user, sessionId)
      ) map authenticateCookie(sessionId)
    } recoverWith authRecovery
  }

  def setFingerPrint(fp: String, ms: Int) = Auth { ctx => me =>
    api.setFingerPrint(ctx.req, FingerPrint(fp)) flatMap {
      _ ?? { hash =>
        !me.lame ?? (for {
          otherIds <- api.recentUserIdsByFingerHash(hash).map(_.filter(me.id!=))
          autoReport <- (otherIds.size >= 2) ?? env.user.repo.countEngines(otherIds).flatMap {
            case nb if nb >= 2 && nb >= otherIds.size / 2 => env.report.api.autoCheatPrintReport(me.id)
            case _ => funit
          }
        } yield ())
      }
    } inject NoContent
  }

  def passwordReset = Open { implicit ctx =>
    forms.passwordResetWithCaptcha map {
      case (form, captcha) => Ok(html.auth.bits.passwordReset(form, captcha))
    }
  }

  def passwordResetApply = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    forms.passwordReset.bindFromRequest.fold(
      err => forms.anyCaptcha map { captcha =>
        BadRequest(html.auth.bits.passwordReset(err, captcha, false.some))
      },
      data => {
        env.user.repo.enabledWithEmail(data.realEmail.normalize) flatMap {
          case Some((user, storedEmail)) => {
            lila.mon.user.auth.passwordResetRequest("success")()
            env.security.passwordReset.send(user, storedEmail) inject Redirect(routes.Auth.passwordResetSent(storedEmail.conceal))
          }
          case _ => {
            lila.mon.user.auth.passwordResetRequest("no_email")()
            forms.passwordResetWithCaptcha map {
              case (form, captcha) => BadRequest(html.auth.bits.passwordReset(form, captcha, false.some))
            }
          }
        }
      }
    )
  }

  def passwordResetSent(email: String) = Open { implicit ctx =>
    fuccess {
      Ok(html.auth.bits.passwordResetSent(email))
    }
  }

  def passwordResetConfirm(token: String) = Open { implicit ctx =>
    env.security.passwordReset confirm token flatMap {
      case None => {
        lila.mon.user.auth.passwordResetConfirm("token_fail")()
        notFound
      }
      case Some(user) => {
        authLog(user.username, "-", "Reset password")
        lila.mon.user.auth.passwordResetConfirm("token_ok")()
        fuccess(html.auth.bits.passwordResetConfirm(user, token, forms.passwdReset, none))
      }
    }
  }

  def passwordResetConfirmApply(token: String) = OpenBody { implicit ctx =>
    env.security.passwordReset confirm token flatMap {
      case None => {
        lila.mon.user.auth.passwordResetConfirm("token_post_fail")()
        notFound
      }
      case Some(user) =>
        implicit val req = ctx.body
        FormFuResult(forms.passwdReset) { err =>
          fuccess(html.auth.bits.passwordResetConfirm(user, token, err, false.some))
        } { data =>
          HasherRateLimit(user.username, ctx.req) { _ =>
            env.user.authenticator.setPassword(user.id, ClearPassword(data.newPasswd1)) >>
              env.user.repo.setEmailConfirmed(user.id).flatMap { _ ?? { e => welcome(user, e) } } >>
              env.user.repo.disableTwoFactor(user.id) >>
              env.security.store.disconnect(user.id) >>
              env.push.webSubscriptionApi.unsubscribeByUser(user) >>
              authenticateUser(user) >>-
              lila.mon.user.auth.passwordResetConfirm("success")()
          }
        }
    }
  }

  def magicLink = Open { implicit ctx =>
    forms.passwordResetWithCaptcha map {
      case (form, captcha) => Ok(html.auth.bits.magicLink(form, captcha))
    }
  }

  def magicLinkApply = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    forms.magicLink.bindFromRequest.fold(
      err => forms.anyCaptcha map { captcha =>
        BadRequest(html.auth.bits.magicLink(err, captcha, false.some))
      },
      data =>
        env.user.repo.enabledWithEmail(data.realEmail.normalize) flatMap {
          case Some((user, storedEmail)) => {
            MagicLinkRateLimit(user, storedEmail, ctx.req) {
              lila.mon.user.auth.magicLinkRequest("success")()
              env.security.magicLink.send(user, storedEmail) inject Redirect(routes.Auth.magicLinkSent(storedEmail.conceal))
            }
          }
          case _ => {
            lila.mon.user.auth.magicLinkRequest("no_email")()
            forms.magicLinkWithCaptcha map {
              case (form, captcha) => BadRequest(html.auth.bits.magicLink(form, captcha, false.some))
            }
          }
        }
    )
  }

  def magicLinkSent(email: String) = Open { implicit ctx =>
    fuccess {
      Ok(html.auth.bits.magicLinkSent(email))
    }
  }

  def magicLinkLogin(token: String) = Open { implicit ctx =>
    env.security.magicLink confirm token flatMap {
      case None => {
        lila.mon.user.auth.magicLinkConfirm("token_fail")()
        notFound
      }
      case Some(user) => {
        authLog(user.username, "-", "Magic link")
        authenticateUser(user) >>-
          lila.mon.user.auth.magicLinkConfirm("success")()
      }
    }
  }

  def makeLoginToken = Auth { _ => me =>
    JsonOk {
      env.security.loginToken generate me map { token =>
        Json.obj(
          "userId" -> me.id,
          "url" -> s"${env.net.baseUrl}${routes.Auth.loginWithToken(token).url}"
        )
      }
    }
  }

  def loginWithToken(token: String) = Open { implicit ctx =>
    Firewall {
      env.security.loginToken consume token flatMap {
        _.fold(notFound)(authenticateUser(_))
      }
    }
  }

  private implicit val limitedDefault = Zero.instance[Result](TooManyRequests("Too many requests, try again later."))

  private[controllers] def HasherRateLimit =
    PasswordHasher.rateLimit[Result](enforce = env.net.rateLimit) _

  private[controllers] def EmailConfirmRateLimit = lila.security.EmailConfirm.rateLimit[Result] _

  private[controllers] def MagicLinkRateLimit = lila.security.MagicLink.rateLimit[Result] _
}
