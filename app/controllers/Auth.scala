package controllers

import play.api.data._, Forms._
import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.mvc._, Results._
import play.api.Play.current

import lila.api.Context
import lila.app._
import lila.common.{ LilaCookie, HTTPRequest }
import lila.user.{ UserRepo, User => UserModel }
import views._

object Auth extends LilaController {

  private def env = Env.security
  private def api = env.api
  private def forms = env.forms

  private def mobileUserOk(u: UserModel): Fu[Result] =
    lila.game.GameRepo urgentGames u map { povs =>
      Ok {
        Env.user.jsonView(u) ++ Json.obj(
          "nowPlaying" -> JsArray(povs take 20 map Env.api.lobbyApi.nowPlaying))
      }
    }

  private def authenticateUser(u: UserModel)(implicit ctx: Context): Fu[Result] = {
    implicit val req = ctx.req
    u.ipBan.fold(
      fuccess(Redirect(routes.Lobby.home)),
      api.saveAuthentication(u.id, ctx.mobileApiVersion) flatMap { sessionId =>
        negotiate(
          html = Redirect {
            get("referrer").filter(_.nonEmpty) orElse req.session.get(api.AccessUri) getOrElse routes.Lobby.home.url
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
    case lila.security.Api.MustConfirmEmail(userId) => UserRepo byId userId map {
      case Some(user) => BadRequest(html.auth.checkYourEmail(user))
      case None       => BadRequest
    }
  }

  def login = Open { implicit ctx =>
    val referrer = get("referrer") orElse {
      getBool("autoref") ?? HTTPRequest.referer(ctx.req)
    }
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
        username => api.loadLoginForm(username) flatMap { loginForm =>
          loginForm.bindFromRequest.fold(
            err => negotiate(
              html = Unauthorized(html.auth.login(err, referrer)).fuccess,
              api = _ => Unauthorized(errorsAsJson(err)).fuccess
            ), {
              case None    => InternalServerError("Authentication error").fuccess
              case Some(u) => authenticateUser(u)
            })
        })
    }
  }

  def logout = Open { implicit ctx =>
    implicit val req = ctx.req
    req.session get "sessionId" foreach lila.security.Store.delete
    negotiate(
      html = fuccess(Redirect(routes.Main.mobile)),
      api = apiVersion => Ok(Json.obj("ok" -> true)).fuccess
    ) map (_ withCookies LilaCookie.newSession)
  }

  def signup = Open { implicit ctx =>
    NoTor {
      Ok(html.auth.signup(forms.signup.website, env.RecaptchaPublicKey)).fuccess
    }
  }

  def signupPost = OpenBody { implicit ctx =>
    implicit val req = ctx.body
    NoTor {
      Firewall {
        negotiate(
          html = forms.signup.website.bindFromRequest.fold(
            err => BadRequest(html.auth.signup(err, env.RecaptchaPublicKey)).fuccess,
            data => env.recaptcha.verify(~data.recaptchaResponse, req).flatMap {
              case false => BadRequest(html.auth.signup(forms.signup.website fill data, env.RecaptchaPublicKey)).fuccess
              case true =>
                lila.mon.user.register.website()
                val email = env.emailAddress.validate(data.email) err s"Invalid email ${data.email}"
                UserRepo.create(data.username, data.password, email.some, ctx.blindMode, none)
                  .flatten(s"No user could be created for ${data.username}")
                  .map(_ -> email).flatMap {
                    case (user, email) => env.emailConfirm.send(user, email) >> {
                      if (env.emailConfirm.effective) Redirect(routes.Auth.checkYourEmail(user.username)).fuccess
                      else redirectNewUser(user)
                    }
                  }
            }),
          api = apiVersion => forms.signup.mobile.bindFromRequest.fold(
            err => fuccess(BadRequest(jsonError(errorsAsJson(err)))),
            data => {
              lila.mon.user.register.mobile()
              val email = data.email flatMap env.emailAddress.validate
              UserRepo.create(data.username, data.password, email, false, apiVersion.some)
                .flatten(s"No user could be created for ${data.username}") flatMap authenticateUser
            }
          )
        )
      }
    }
  }

  def checkYourEmail(name: String) = Open { implicit ctx =>
    OptionOk(UserRepo named name) { user =>
      html.auth.checkYourEmail(user)
    }
  }

  def signupConfirmEmail(token: String) = Open { implicit ctx =>
    Env.security.emailConfirm.confirm(token) flatMap {
      _.fold(notFound)(redirectNewUser)
    }
  }

  private def redirectNewUser(user: UserModel)(implicit ctx: Context) = {
    implicit val req = ctx.req
    api.saveAuthentication(user.id, ctx.mobileApiVersion) map { sessionId =>
      Redirect(routes.User.show(user.username)) withCookies LilaCookie.session("sessionId", sessionId)
    } recoverWith authRecovery
  }

  private def noTorResponse(implicit ctx: Context) = negotiate(
    html = Unauthorized(html.auth.tor()).fuccess,
    api = _ => Unauthorized(jsonError("Can't login from Tor, sorry!")).fuccess)

  def setFingerprint(fp: String, ms: Int) = Auth { ctx =>
    me =>
      api.setFingerprint(ctx.req, fp) flatMap {
        _ ?? { hash =>
          !me.lame ?? {
            api.recentUserIdsByFingerprint(hash).map(_.filter(me.id!=)) flatMap {
              case otherIds if otherIds.size >= 2 => UserRepo countEngines otherIds flatMap {
                case nb if nb >= 2 && nb >= otherIds.size / 2 => Env.report.api.autoCheatPrintReport(me.id)
                case _                                        => funit
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
        val email = env.emailAddress.validate(data.email) | data.email
        UserRepo enabledByEmail email flatMap {
          case Some(user) =>
            Env.security.passwordReset.send(user, email) inject Redirect(routes.Auth.passwordResetSent(data.email))
          case _ => forms.passwordResetWithCaptcha map {
            case (form, captcha) => BadRequest(html.auth.passwordReset(form, captcha, false.some))
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
      case Some(user) =>
        fuccess(html.auth.passwordResetConfirm(user, token, forms.passwdReset, none))
      case _ => notFound
    }
  }

  def passwordResetConfirmApply(token: String) = OpenBody { implicit ctx =>
    Env.security.passwordReset confirm token flatMap {
      case Some(user) =>
        implicit val req = ctx.body
        FormFuResult(forms.passwdReset) { err =>
          fuccess(html.auth.passwordResetConfirm(user, token, err, false.some))
        } { data =>
          UserRepo.passwd(user.id, data.newPasswd1) >> authenticateUser(user)
        }
      case _ => notFound
    }
  }
}
