package controllers

import play.api.i18n.Messages.Implicits._
import play.api.libs.json._
import play.api.mvc._
import play.api.Play.current

import lila.api.Context
import lila.app._
import lila.common.{ LilaCookie, HTTPRequest, IpAddress }
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
    case lila.security.Api.MustConfirmEmail(userId) => UserRepo byId userId map {
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
        username => api.loadLoginForm(username) flatMap { loginForm =>
          loginForm.bindFromRequest.fold(
            err => negotiate(
              html = Unauthorized(html.auth.login(err, referrer)).fuccess,
              api = _ => Unauthorized(errorsAsJson(err)).fuccess
            ), {
              case None => InternalServerError("Authentication error").fuccess
              case Some(u) => authenticateUser(u)
            }
          )
        }
      )
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

  private def mustConfirmEmailByIP(ip: IpAddress, username: String): Fu[Boolean] =
    api.recentByIpExists(ip) >>|
      Mod.ipIntelCache.get(ip).map(80 <).recover { case _: Exception => false }

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
                mustConfirmEmailByIP(
                  ip = HTTPRequest lastRemoteAddress ctx.req,
                  username = data.username
                ) flatMap { mustConfirmEmail =>
                  lila.mon.user.register.website()
                  lila.mon.user.register.mustConfirmEmail(mustConfirmEmail)()
                  val email = env.emailAddressValidator.validate(data.realEmail) err s"Invalid email ${data.email}"
                  UserRepo.create(data.username, data.password, email, ctx.blindMode, none,
                    mustConfirmEmail = mustConfirmEmail)
                    .flatten(s"No user could be created for ${data.username}")
                    .map(_ -> email).flatMap {
                      case (user, email) if mustConfirmEmail =>
                        env.emailConfirm.send(user, email) >> {
                          if (env.emailConfirm.effective) Redirect(routes.Auth.checkYourEmail(user.username)).fuccess
                          else redirectNewUser(user)
                        }
                      case (user, email) => redirectNewUser(user)
                    }
                }
            }
          ),
          api = apiVersion => forms.signup.mobile.bindFromRequest.fold(
            err => fuccess(BadRequest(jsonError(errorsAsJson(err)))),
            data => {
              val mustConfirmEmail = ~data.canConfirm
              lila.mon.user.register.mobile()
              lila.mon.user.register.mustConfirmEmail(mustConfirmEmail)()
              val email = env.emailAddressValidator.validate(data.realEmail) err s"Invalid email ${data.email}"
              UserRepo.create(data.username, data.password, email, false, apiVersion.some,
                mustConfirmEmail = mustConfirmEmail)
                .flatten(s"No user could be created for ${data.username}")
                .map(_ -> email).flatMap {
                  case (user, email) if mustConfirmEmail =>
                    env.emailConfirm.send(user, email) >> {
                      if (env.emailConfirm.effective) Ok(Json.obj("email_confirm" -> true)).fuccess
                      else authenticateUser(user)
                    }
                  case (user, _) => authenticateUser(user)
                }
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
      case None =>
        lila.mon.user.register.confirmEmailResult(false)()
        notFound
      case Some(user) =>
        lila.mon.user.register.confirmEmailResult(true)()
        redirectNewUser(user)
    }
  }

  private def redirectNewUser(user: UserModel)(implicit ctx: Context) = {
    implicit val req = ctx.req
    api.saveAuthentication(user.id, ctx.mobileApiVersion) map { sessionId =>
      Redirect(routes.User.show(user.username)) withCookies LilaCookie.session("sessionId", sessionId)
    } recoverWith authRecovery
  }

  def setFingerprint(fp: String, ms: Int) = Auth { ctx => me =>
    api.setFingerprint(ctx.req, fp) flatMap {
      _ ?? { hash =>
        !me.lame ?? {
          api.recentUserIdsByFingerprint(hash).map(_.filter(me.id!=)) flatMap {
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
      _ ?? { user =>
        fuccess(html.auth.passwordResetConfirm(user, token, forms.passwdReset, none))
      }
    }
  }

  def passwordResetConfirmApply(token: String) = OpenBody { implicit ctx =>
    Env.security.passwordReset confirm token flatMap {
      _ ?? { user =>
        implicit val req = ctx.body
        FormFuResult(forms.passwdReset) { err =>
          fuccess(html.auth.passwordResetConfirm(user, token, err, false.some))
        } { data =>
          UserRepo.passwd(user.id, data.newPasswd1) >>
            env.store.disconnect(user.id) >>
            authenticateUser(user)
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
}
