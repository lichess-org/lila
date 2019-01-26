package controllers

import ornicar.scalalib.Zero
import play.api.data.FormError
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.common.{ LilaCookie, HTTPRequest, IpAddress, EmailAddress }
import lila.memo.RateLimit
import lila.security.FingerPrint
import lila.user.{ UserRepo, User => UserModel, PasswordHasher }
import UserModel.ClearPassword
import views._

object Auth extends LilaController {

  private def env = Env.security
  private def api = env.api
  private def forms = env.forms

  private def mobileUserOk(u: UserModel, sessionId: String): Fu[Result] =
    lila.game.GameRepo urgentGames u map { povs =>
      Ok {
        Env.user.jsonView(u) ++ Json.obj(
          "nowPlaying" -> JsArray(povs take 20 map Env.api.lobbyApi.nowPlaying),
          "sessionId" -> sessionId
        )
      }
    }

  private val refRegex = """[\w@/-]++""".r

  private def goodReferrer(referrer: String): Boolean = {
    referrer.nonEmpty &&
      referrer.stripPrefix("/") != "mobile" && {
        (!referrer.contains("//") && refRegex.matches(referrer)) ||
          referrer.startsWith(Env.oAuth.baseUrl)
      }
  }

  def authenticateUser(u: UserModel, result: Option[String => Result] = None)(implicit ctx: Context): Fu[Result] = {
    implicit val req = ctx.req
    if (u.ipBan) fuccess(Redirect(routes.Lobby.home))
    else api.saveAuthentication(u.id, ctx.mobileApiVersion) flatMap { sessionId =>
      negotiate(
        html = fuccess {
          val redirectTo = get("referrer").filter(goodReferrer) orElse
            req.session.get(api.AccessUri) getOrElse
            routes.Lobby.home.url
          result.fold(Redirect(redirectTo))(_(redirectTo))
        },
        api = _ => mobileUserOk(u, sessionId)
      ) map authenticateCookie(sessionId)
    } recoverWith authRecovery
  }

  private def authenticateCookie(sessionId: String)(result: Result)(implicit req: RequestHeader) =
    result.withCookies(
      LilaCookie.withSession {
        _ + ("sessionId" -> sessionId) - api.AccessUri - lila.security.EmailConfirm.cookie.name
      }
    )

  private def authRecovery(implicit ctx: Context): PartialFunction[Throwable, Fu[Result]] = {
    case lila.security.SecurityApi.MustConfirmEmail(_) => fuccess {
      if (HTTPRequest isXhr ctx.req) Ok(s"ok:${routes.Auth.checkYourEmail}")
      else BadRequest(Account.renderCheckYourEmail)
    }
  }

  def login = Open { implicit ctx =>
    val referrer = get("referrer").filter(goodReferrer)
    Ok(html.auth.login(api.loginForm, referrer)).fuccess
  }

  private val is2fa = Set("MissingTotpToken", "InvalidTotpToken")

  def authenticate = OpenBody { implicit ctx =>
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
                  UserRepo.email(u.id) foreach {
                    _ foreach { garbageCollect(u, _) }
                  }
                  authenticateUser(u, Some(redirectTo => Ok(s"ok:$redirectTo")))
              }
            )
          }
        }
      )
    }, Ok(s"ok:/").fuccess)
  }

  def logout = Open { implicit ctx =>
    implicit val req = ctx.req
    req.session get "sessionId" foreach lila.security.Store.delete
    negotiate(
      html = Redirect(routes.Main.mobile).fuccess,
      api = _ => Ok(Json.obj("ok" -> true)).fuccess
    ) map (_ withCookies LilaCookie.newSession)
  }

  // mobile app BC logout with GET
  def logoutGet = Open { implicit ctx =>
    implicit val req = ctx.req
    negotiate(
      html = notFound,
      api = _ => {
        req.session get "sessionId" foreach lila.security.Store.delete
        Ok(Json.obj("ok" -> true)).withCookies(LilaCookie.newSession).fuccess
      }
    )
  }

  def signup = Open { implicit ctx =>
    NoTor {
      Ok(html.auth.signup(forms.signup.website, env.recaptchaPublicConfig)).fuccess
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
            else Env.security.ipTrust.isSuspicious(ip).map {
              case true => YesBecauseIpSusp
              case _ => Nope
            }
          }
        }
      }
    }
  }

  private def authLog(user: String, msg: String) = lila.log("auth").info(s"$user $msg")

  def signupPost = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    NoTor {
      Firewall {
        forms.preloadEmailDns >> negotiate(
          html = forms.signup.website.bindFromRequest.fold(
            err => {
              err("username").value foreach { authLog(_, s"Signup fail: ${err.errors mkString ", "}") }
              BadRequest(html.auth.signup(err, env.recaptchaPublicConfig)).fuccess
            },
            data => env.recaptcha.verify(~data.recaptchaResponse, req).flatMap {
              case false =>
                authLog(data.username, "Signup recaptcha fail")
                BadRequest(html.auth.signup(forms.signup.website fill data, env.recaptchaPublicConfig)).fuccess
              case true => HasherRateLimit(data.username, ctx.req) { _ =>
                MustConfirmEmail(data.fingerPrint) flatMap { mustConfirm =>
                  lila.mon.user.register.website()
                  lila.mon.user.register.mustConfirmEmail(mustConfirm.toString)()
                  val email = env.emailAddressValidator.validate(data.realEmail) err s"Invalid email ${data.email}"
                  authLog(data.username, s"$email fp: ${data.fingerPrint} mustConfirm: $mustConfirm req:${ctx.req}")
                  val passwordHash = Env.user.authenticator passEnc ClearPassword(data.password)
                  UserRepo.create(data.username, passwordHash, email, ctx.blind, none,
                    mustConfirmEmail = mustConfirm.value)
                    .flatten(s"No user could be created for ${data.username}")
                    .map(_ -> email).flatMap {
                      case (user, email) if mustConfirm.value =>
                        env.emailConfirm.send(user, email) >> {
                          if (env.emailConfirm.effective)
                            api.saveSignup(user.id, ctx.mobileApiVersion, data.fingerPrint) inject {
                              Redirect(routes.Auth.checkYourEmail) withCookies
                                lila.security.EmailConfirm.cookie.make(user, email)(ctx.req)
                            }
                          else welcome(user, email) >> redirectNewUser(user)
                        }
                      case (user, email) => welcome(user, email) >> redirectNewUser(user)
                    }
                }
              }
            }
          ),
          api = apiVersion => forms.signup.mobile.bindFromRequest.fold(
            err => {
              err("username").value foreach { authLog(_, s"Signup fail: ${err.errors mkString ", "}") }
              jsonFormError(err)
            },
            data => HasherRateLimit(data.username, ctx.req) { _ =>
              val email = env.emailAddressValidator.validate(data.realEmail) err s"Invalid email ${data.email}"
              val mustConfirm = MustConfirmEmail.YesBecauseMobile
              lila.mon.user.register.mobile()
              lila.mon.user.register.mustConfirmEmail(mustConfirm.toString)()
              authLog(data.username, s"Signup mobile must confirm email: $mustConfirm")
              val passwordHash = Env.user.authenticator passEnc ClearPassword(data.password)
              UserRepo.create(data.username, passwordHash, email, false, apiVersion.some,
                mustConfirmEmail = mustConfirm.value)
                .flatten(s"No user could be created for ${data.username}")
                .map(_ -> email).flatMap {
                  case (user, email) if mustConfirm.value =>
                    env.emailConfirm.send(user, email) >> {
                      if (env.emailConfirm.effective) Ok(Json.obj("email_confirm" -> true)).fuccess
                      else welcome(user, email) >> authenticateUser(user)
                    }
                  case (user, _) => welcome(user, email) >> authenticateUser(user)
                }
            }
          )
        )
      }
    }
  }

  private def welcome(user: UserModel, email: EmailAddress)(implicit ctx: Context) = {
    garbageCollect(user, email)
    env.welcomeEmail(user, email)
  }

  private def garbageCollect(user: UserModel, email: EmailAddress)(implicit ctx: Context) =
    Env.security.garbageCollector.delay(user, email, ctx.req)

  def checkYourEmail = Open { implicit ctx =>
    ctx.me match {
      case Some(me) => Redirect(routes.User.show(me.username)).fuccess
      case None => lila.security.EmailConfirm.cookie get ctx.req match {
        case None => Ok(Account.renderCheckYourEmail).fuccess
        case Some(userEmail) =>
          UserRepo nameExists userEmail.username map {
            case false => Redirect(routes.Auth.signup) withCookies LilaCookie.newSession(ctx.req)
            case true => Ok(Account.renderCheckYourEmail)
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
        email => UserRepo.named(userEmail.username) flatMap {
          _.fold(Redirect(routes.Auth.signup).fuccess) { user =>
            UserRepo.mustConfirmEmail(user.id) flatMap {
              case false => Redirect(routes.Auth.login).fuccess
              case _ =>
                val newUserEmail = userEmail.copy(email = EmailAddress(email))
                EmailConfirmRateLimit(newUserEmail, ctx.req) {
                  lila.mon.email.types.fix()
                  UserRepo.email(user.id, newUserEmail.email) >>
                    env.emailConfirm.send(user, newUserEmail.email) inject {
                      Redirect(routes.Auth.checkYourEmail) withCookies
                        lila.security.EmailConfirm.cookie.make(user, newUserEmail.email)(ctx.req)
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
    Env.security.emailConfirm.confirm(token) flatMap {
      case Result.NotFound =>
        lila.mon.user.register.confirmEmailResult(false)()
        notFound
      case Result.AlreadyConfirmed(user) if ctx.is(user) =>
        Redirect(routes.User.show(user.username)).fuccess
      case Result.AlreadyConfirmed(user) =>
        Redirect(routes.Auth.login).fuccess
      case Result.JustConfirmed(user) =>
        lila.mon.user.register.confirmEmailResult(true)()
        UserRepo.email(user.id).flatMap {
          _.?? { email =>
            authLog(user.username, s"Confirmed email $email")
            welcome(user, email)
          }
        } >> redirectNewUser(user)
    }
  }

  private def redirectNewUser(user: UserModel)(implicit ctx: Context) = {
    implicit val req = ctx.req
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
        !me.lame ?? {
          api.recentUserIdsByFingerHash(hash).map(_.filter(me.id!=)) flatMap {
            case otherIds if otherIds.size >= 2 => UserRepo countEngines otherIds flatMap {
              case nb if nb >= 2 && nb >= otherIds.size / 2 => Env.report.api.autoCheatPrintReport(me.id)
              case _ => funit
            }
            case _ => funit
          }
        }
      }
    } inject NoContent
  }

  def passwordReset = Open { implicit ctx =>
    forms.passwordResetWithCaptcha map {
      case (form, captcha) => Ok(html.auth.passwordReset(form, captcha))
    }
  }

  def passwordResetApply = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    forms.passwordReset.bindFromRequest.fold(
      err => forms.anyCaptcha map { captcha =>
        BadRequest(html.auth.passwordReset(err, captcha, false.some))
      },
      data => {
        val email = env.emailAddressValidator.validate(data.realEmail) | data.realEmail
        UserRepo enabledByEmail email flatMap {
          case Some(user) => {
            lila.mon.user.auth.passwordResetRequest("success")()
            Env.security.passwordReset.send(user, email) inject Redirect(routes.Auth.passwordResetSent(data.email))
          }
          case _ => {
            lila.mon.user.auth.passwordResetRequest("no_email")()
            forms.passwordResetWithCaptcha map {
              case (form, captcha) => BadRequest(html.auth.passwordReset(form, captcha, false.some))
            }
          }
        }
      }
    )
  }

  def passwordResetSent(email: String) = Open { implicit ctx =>
    fuccess {
      Ok(html.auth.passwordResetSent(email))
    }
  }

  def passwordResetConfirm(token: String) = Open { implicit ctx =>
    Env.security.passwordReset confirm token flatMap {
      case None => {
        lila.mon.user.auth.passwordResetConfirm("token_fail")()
        notFound
      }
      case Some(user) => {
        authLog(user.username, "Reset password")
        lila.mon.user.auth.passwordResetConfirm("token_ok")()
        fuccess(html.auth.passwordResetConfirm(user, token, forms.passwdReset, none))
      }
    }
  }

  def passwordResetConfirmApply(token: String) = OpenBody { implicit ctx =>
    Env.security.passwordReset confirm token flatMap {
      case None => {
        lila.mon.user.auth.passwordResetConfirm("token_post_fail")()
        notFound
      }
      case Some(user) =>
        implicit val req = ctx.body
        FormFuResult(forms.passwdReset) { err =>
          fuccess(html.auth.passwordResetConfirm(user, token, err, false.some))
        } { data =>
          HasherRateLimit(user.username, ctx.req) { _ =>
            Env.user.authenticator.setPassword(user.id, ClearPassword(data.newPasswd1)) >>
              UserRepo.setEmailConfirmed(user.id) >>
              env.store.disconnect(user.id) >>
              authenticateUser(user) >>-
              lila.mon.user.auth.passwordResetConfirm("success")()
          }
        }
    }
  }

  def makeLoginToken = Auth { implicit ctx => me =>
    JsonOk {
      env.loginToken generate me map { token =>
        Json.obj(
          "userId" -> me.id,
          "url" -> s"${Env.api.Net.BaseUrl}${routes.Auth.loginWithToken(token).url}"
        )
      }
    }
  }

  def loginWithToken(token: String) = Open { implicit ctx =>
    Firewall {
      env.loginToken consume token flatMap {
        _.fold(notFound)(authenticateUser(_))
      }
    }
  }

  private implicit val limitedDefault = Zero.instance[Result](TooManyRequest)

  private[controllers] def HasherRateLimit =
    PasswordHasher.rateLimit[Result](enforce = Env.api.Net.RateLimit) _

  private[controllers] def EmailConfirmRateLimit = lila.security.EmailConfirm.rateLimit[Result] _
}
