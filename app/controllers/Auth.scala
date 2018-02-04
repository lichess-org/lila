package controllers

import ornicar.scalalib.Zero
import play.api.libs.json._
import play.api.mvc._
import scala.concurrent.duration._

import lila.api.Context
import lila.app._
import lila.common.{ LilaCookie, HTTPRequest, IpAddress, EmailAddress }
import lila.memo.RateLimit
import lila.security.FingerPrint
import lila.user.{ UserRepo, User => UserModel }
import UserModel.ClearPassword
import views._

object Auth extends LilaController {

  private def env = Env.security
  private def api = env.api
  private def forms = env.forms

  private def mobileUserOk(u: UserModel): Fu[Result] =
    lila.game.GameRepo urgentGames u map { povs =>
      Ok {
        Env.user.jsonView(u) ++ Json.obj(
          "nowPlaying" -> JsArray(povs take 20 map Env.api.lobbyApi.nowPlaying)
        )
      }
    }

  private def goodReferrer(referrer: String): Boolean = {
    referrer.nonEmpty &&
      referrer.stripPrefix("/") != "mobile" &&
      """(?:[\w@-]|(:?\/[\w@-]))*\/?""".r.matches(referrer)
  }

  def authenticateUser(u: UserModel, result: Option[Fu[Result]] = None)(implicit ctx: Context): Fu[Result] = {
    implicit val req = ctx.req
    u.ipBan.fold(
      fuccess(Redirect(routes.Lobby.home)),
      api.saveAuthentication(u.id, ctx.mobileApiVersion) flatMap { sessionId =>
        negotiate(
          html = result | Redirect {
            get("referrer").filter(goodReferrer) orElse
              req.session.get(api.AccessUri) getOrElse
              routes.Lobby.home.url
          }.fuccess,
          api = _ => mobileUserOk(u)
        ) map {
            _ withCookies LilaCookie.withSession { session =>
              session + ("sessionId" -> sessionId) - api.AccessUri
            }
          }
      } recoverWith authRecovery
    )
  }

  private def authRecovery(implicit ctx: Context): PartialFunction[Throwable, Fu[Result]] = {
    case lila.security.SecurityApi.MustConfirmEmail(userId) => UserRepo byId userId map {
      case Some(user) => BadRequest(html.auth.checkYourEmail(user))
      case None => BadRequest
    }
  }

  def login = Open { implicit ctx =>
    val referrer = get("referrer")
    Ok(html.auth.login(api.loginForm, referrer)).fuccess
  }

  def authenticate = OpenBody { implicit ctx =>
    Firewall {
      implicit val req = ctx.body
      val referrer = get("referrer")
      api.usernameForm.bindFromRequest.fold(
        err => negotiate(
          html = Unauthorized(html.auth.login(api.loginForm, referrer)).fuccess,
          api = _ => Unauthorized(errorsAsJson(err)).fuccess
        ),
        username => HasherRateLimit(username, ctx.req) { chargeIpLimiter =>
          api.loadLoginForm(username) flatMap { loginForm =>
            loginForm.bindFromRequest.fold(
              err => {
                chargeIpLimiter(1)
                negotiate(
                  html = Unauthorized(html.auth.login(err, referrer)).fuccess,
                  api = _ => Unauthorized(errorsAsJson(err)).fuccess
                )
              }, {
                case None => InternalServerError("Authentication error").fuccess
                case Some(u) =>
                  UserRepo.email(u.id) foreach {
                    _ foreach { garbageCollect(u, _) }
                  }
                  authenticateUser(u)
              }
            )
          }
        }
      )
    }
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
      Ok(html.auth.signup(forms.signup.website, env.RecaptchaPublicKey)).fuccess
    }
  }

  private sealed abstract class MustConfirmEmail(val value: Boolean)
  private object MustConfirmEmail {

    case object Nope extends MustConfirmEmail(false)
    case object YesBecausePrint extends MustConfirmEmail(true)
    case object YesBecauseIp extends MustConfirmEmail(true)
    case object YesBecauseProxy extends MustConfirmEmail(true)
    case object YesBecauseMobile extends MustConfirmEmail(true)
    case object YesBecauseUA extends MustConfirmEmail(true)

    def apply(print: Option[FingerPrint])(implicit ctx: Context): Fu[MustConfirmEmail] = {
      val ip = HTTPRequest lastRemoteAddress ctx.req
      api.recentByIpExists(ip) flatMap { ipExists =>
        if (ipExists) fuccess(YesBecauseIp)
        else print.??(api.recentByPrintExists) flatMap { printExists =>
          if (printExists) fuccess(YesBecausePrint)
          else if (HTTPRequest weirdUA ctx.req) fuccess(YesBecauseUA)
          else Env.security.ipIntel(ip).map(80 <).map { _.fold(YesBecauseProxy, Nope) }
        }
      }
    }
  }

  private def authLog(user: String, msg: String) = lila.log("auth").info(s"$user $msg")

  def signupPost = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    NoTor {
      Firewall {
        negotiate(
          html = forms.signup.website.bindFromRequest.fold(
            err => {
              err("username").value foreach { authLog(_, s"Signup fail: ${err.errors mkString ", "}") }
              BadRequest(html.auth.signup(err, env.RecaptchaPublicKey)).fuccess
            },
            data => env.recaptcha.verify(~data.recaptchaResponse, req).flatMap {
              case false =>
                authLog(data.username, "Signup recaptcha fail")
                BadRequest(html.auth.signup(forms.signup.website fill data, env.RecaptchaPublicKey)).fuccess
              case true => HasherRateLimit(data.username, ctx.req) { _ =>
                MustConfirmEmail(data.fingerPrint) flatMap { mustConfirm =>
                  authLog(data.username, s"fp: ${data.fingerPrint} mustConfirm: $mustConfirm req:${ctx.req}")
                  lila.mon.user.register.website()
                  lila.mon.user.register.mustConfirmEmail(mustConfirm.value)()
                  authLog(data.username, s"Signup website must confirm email: $mustConfirm")
                  val email = env.emailAddressValidator.validate(data.realEmail) err s"Invalid email ${data.email}"
                  val passwordHash = Env.user.authenticator passEnc ClearPassword(data.password)
                  UserRepo.create(data.username, passwordHash, email, ctx.blindMode, none,
                    mustConfirmEmail = mustConfirm.value)
                    .flatten(s"No user could be created for ${data.username}")
                    .map(_ -> email).flatMap {
                      case (user, email) if mustConfirm.value =>
                        env.emailConfirm.send(user, email) >> {
                          if (env.emailConfirm.effective)
                            api.saveSignup(user.id, ctx.mobileApiVersion) inject
                              Redirect(routes.Auth.checkYourEmail(user.username))
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
              fuccess(BadRequest(jsonError(errorsAsJson(err))))
            },
            data => HasherRateLimit(data.username, ctx.req) { _ =>
              fuccess(MustConfirmEmail.YesBecauseMobile) flatMap { mustConfirm =>
                lila.mon.user.register.mobile()
                lila.mon.user.register.mustConfirmEmail(mustConfirm.value)()
                authLog(data.username, s"Signup mobile must confirm email: $mustConfirm")
                val email = env.emailAddressValidator.validate(data.realEmail) err s"Invalid email ${data.email}"
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
    Env.security.garbageCollector.delay(user, HTTPRequest lastRemoteAddress ctx.req, email)

  def checkYourEmail(name: String) = Open { implicit ctx =>
    OptionOk(UserRepo named name) { user =>
      html.auth.checkYourEmail(user)
    }
  }

  def signupConfirmEmail(token: String) = Open { implicit ctx =>
    Env.security.emailConfirm.confirm(token) flatMap {
      case None =>
        lila.mon.user.register.confirmEmailResult(false)()
        notFound
      case Some(user) =>
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
        api = _ => mobileUserOk(user)
      ) map {
        _ withCookies LilaCookie.session("sessionId", sessionId)
      }
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
    } inject Ok
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

  private[controllers] def HasherRateLimit = lila.user.PasswordHasher.rateLimit[Result] _
}
