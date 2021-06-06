package controllers

import ornicar.scalalib.Zero
import play.api.data.FormError
import play.api.libs.json._
import play.api.mvc._
import scala.annotation.nowarn
import scala.concurrent.duration._
import views._

import lila.api.Context
import lila.app._
import lila.common.{ EmailAddress, HTTPRequest }
import lila.memo.RateLimit
import lila.security.SecurityForm.{ MagicLink, PasswordReset }
import lila.security.{ FingerPrint, Signup }
import lila.user.User.ClearPassword
import lila.user.{ User => UserModel, PasswordHasher }

final class Auth(
    env: Env,
    accountC: => Account
) extends LilaController(env) {

  private def api   = env.security.api
  private def forms = env.security.forms

  private def mobileUserOk(u: UserModel, sessionId: String): Fu[Result] =
    env.round.proxyRepo urgentGames u map { povs =>
      Ok {
        env.user.jsonView(u) ++ Json.obj(
          "nowPlaying" -> JsArray(povs take 20 map env.api.lobbyApi.nowPlaying),
          "sessionId"  -> sessionId
        )
      }
    }

  private def getReferrer(implicit ctx: Context) =
    get("referrer").filter(env.api.referrerRedirect.valid) orElse
      ctxReq.session.get(api.AccessUri) getOrElse
      routes.Lobby.home.url

  def authenticateUser(u: UserModel, result: Option[String => Result] = None)(implicit
      ctx: Context
  ): Fu[Result] =
    api.saveAuthentication(u.id, ctx.mobileApiVersion) flatMap { sessionId =>
      negotiate(
        html = fuccess {
          result.fold(Redirect(getReferrer))(_(getReferrer))
        },
        api = _ => mobileUserOk(u, sessionId)
      ) map authenticateCookie(sessionId)
    } recoverWith authRecovery

  private def authenticateAppealUser(u: UserModel, redirect: String => Result)(implicit
      ctx: Context
  ): Fu[Result] =
    api.appeal.saveAuthentication(u.id) flatMap { sessionId =>
      negotiate(
        html = redirect(routes.Appeal.landing.url).fuccess map authenticateCookie(sessionId),
        api = _ => NotFound.fuccess
      )
    } recoverWith authRecovery

  private def authenticateCookie(sessionId: String)(result: Result)(implicit req: RequestHeader) =
    result.withCookies(
      env.lilaCookie.withSession {
        _ + (api.sessionIdKey -> sessionId) - api.AccessUri - lila.security.EmailConfirm.cookie.name
      }
    )

  private def authRecovery(implicit ctx: Context): PartialFunction[Throwable, Fu[Result]] = {
    case lila.security.SecurityApi.MustConfirmEmail(_) =>
      fuccess {
        if (HTTPRequest isXhr ctx.req) Ok(s"ok:${routes.Auth.checkYourEmail}")
        else BadRequest(accountC.renderCheckYourEmail)
      }
  }

  def login =
    Open { implicit ctx =>
      val referrer = get("referrer").filter(env.api.referrerRedirect.valid)
      referrer ifTrue ctx.isAuth match {
        case Some(url) => Redirect(url).fuccess // redirect immediately if already logged in
        case None      => Ok(html.auth.login(api.loginForm, referrer)).fuccess
      }
    }

  private val is2fa = Set("MissingTotpToken", "InvalidTotpToken")

  def authenticate =
    OpenBody { implicit ctx =>
      def redirectTo(url: String) = if (HTTPRequest isXhr ctx.req) Ok(s"ok:$url") else Redirect(url)
      Firewall {
        implicit val req = ctx.body
        val referrer     = get("referrer").filterNot(env.api.referrerRedirect.sillyLoginReferrers.contains)
        api.usernameOrEmailForm
          .bindFromRequest()
          .fold(
            err =>
              negotiate(
                html = Unauthorized(html.auth.login(api.loginForm, referrer)).fuccess,
                api = _ => Unauthorized(ridiculousBackwardCompatibleJsonError(errorsAsJson(err))).fuccess
              ),
            usernameOrEmail =>
              HasherRateLimit(usernameOrEmail, ctx.req) { chargeIpLimiter =>
                api.loadLoginForm(usernameOrEmail) flatMap {
                  _.bindFromRequest()
                    .fold(
                      err => {
                        chargeIpLimiter(1)
                        negotiate(
                          html = fuccess {
                            err.errors match {
                              case List(FormError("", List(err), _)) if is2fa(err) => Ok(err)
                              case _                                               => Unauthorized(html.auth.login(err, referrer))
                            }
                          },
                          api = _ =>
                            Unauthorized(ridiculousBackwardCompatibleJsonError(errorsAsJson(err))).fuccess
                        )
                      },
                      result =>
                        result.toOption match {
                          case None => InternalServerError("Authentication error").fuccess
                          case Some(u) if u.disabled =>
                            negotiate(
                              html = env.mod.logApi.closedByMod(u) flatMap {
                                case true => authenticateAppealUser(u, redirectTo)
                                case _    => redirectTo(routes.Account.reopen.url).fuccess
                              },
                              api = _ => Unauthorized(jsonError("This account is closed.")).fuccess
                            )
                          case Some(u) =>
                            env.user.repo.email(u.id) foreach {
                              _ foreach { garbageCollect(u, _) }
                            }
                            authenticateUser(u, Some(redirectTo))
                        }
                    )
                }
              }(rateLimitedFu)
          )
      }
    }

  def logout =
    Open { implicit ctx =>
      val currentSessionId = ~env.security.api.reqSessionId(ctx.req)
      env.security.store.delete(currentSessionId) >>
        env.push.webSubscriptionApi.unsubscribeBySession(currentSessionId) >>
        negotiate(
          html = Redirect(routes.Auth.login).fuccess,
          api = _ => Ok(Json.obj("ok" -> true)).fuccess
        ).dmap(_.withCookies(env.lilaCookie.newSession))
    }

  // mobile app BC logout with GET
  def logoutGet =
    Auth { implicit ctx => _ =>
      negotiate(
        html = Ok(html.auth.bits.logout()).fuccess,
        api = _ => {
          ctxReq.session get api.sessionIdKey foreach env.security.store.delete
          Ok(Json.obj("ok" -> true)).withCookies(env.lilaCookie.newSession).fuccess
        }
      )
    }

  def signup =
    Open { implicit ctx =>
      NoTor {
        Ok(html.auth.signup(forms.signup.website)).fuccess
      }
    }

  private def authLog(user: String, email: String, msg: String) =
    lila.log("auth").info(s"$user $email $msg")

  def signupPost =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      NoTor {
        Firewall {
          forms.preloadEmailDns >> negotiate(
            html = env.security.signup
              .website(ctx.blind)
              .flatMap {
                case Signup.RateLimited => limitedDefault.zero.fuccess
                case Signup.MissingCaptcha =>
                  BadRequest(html.auth.signup(forms.signup.website)).fuccess
                case Signup.Bad(err) =>
                  BadRequest(html.auth.signup(forms.signup.website withForm err)).fuccess
                case Signup.ConfirmEmail(user, email) =>
                  fuccess {
                    Redirect(routes.Auth.checkYourEmail) withCookies
                      lila.security.EmailConfirm.cookie
                        .make(env.lilaCookie, user, email)(ctx.req)
                  }
                case Signup.AllSet(user, email) =>
                  welcome(user, email, sendWelcomeEmail = true) >> redirectNewUser(user)
              },
            api = apiVersion =>
              env.security.signup
                .mobile(apiVersion)
                .flatMap {
                  case Signup.RateLimited        => limitedDefault.zero.fuccess
                  case Signup.MissingCaptcha     => fuccess(BadRequest(jsonError("Missing captcha?!")))
                  case Signup.Bad(err)           => jsonFormError(err)
                  case Signup.ConfirmEmail(_, _) => Ok(Json.obj("email_confirm" -> true)).fuccess
                  case Signup.AllSet(user, email) =>
                    welcome(user, email, sendWelcomeEmail = true) >> authenticateUser(user)
                }
          )
        }
      }
    }

  private def welcome(user: UserModel, email: EmailAddress, sendWelcomeEmail: Boolean)(implicit
      ctx: Context
  ): Funit = {
    garbageCollect(user, email)
    if (sendWelcomeEmail) env.mailer.automaticEmail.welcomeEmail(user, email)
    env.mailer.automaticEmail.welcomePM(user)
    env.pref.api.saveNewUserPrefs(user, ctx.req)
  }

  private def garbageCollect(user: UserModel, email: EmailAddress)(implicit ctx: Context) =
    env.security.garbageCollector.delay(user, email, ctx.req)

  def checkYourEmail =
    Open { implicit ctx =>
      RedirectToProfileIfLoggedIn {
        lila.security.EmailConfirm.cookie get ctx.req match {
          case None => Ok(accountC.renderCheckYourEmail).fuccess
          case Some(userEmail) =>
            env.user.repo nameExists userEmail.username map {
              case false => Redirect(routes.Auth.signup) withCookies env.lilaCookie.newSession(ctx.req)
              case true  => Ok(accountC.renderCheckYourEmail)
            }
        }
      }
    }

  // after signup and before confirmation
  def fixEmail =
    OpenBody { implicit ctx =>
      lila.security.EmailConfirm.cookie.get(ctx.req) ?? { userEmail =>
        implicit val req = ctx.body
        forms.preloadEmailDns >> forms
          .fixEmail(userEmail.email)
          .bindFromRequest()
          .fold(
            err => BadRequest(html.auth.checkYourEmail(userEmail.some, err.some)).fuccess,
            email =>
              env.user.repo.named(userEmail.username) flatMap {
                _.fold(Redirect(routes.Auth.signup).fuccess) { user =>
                  env.user.repo.mustConfirmEmail(user.id) flatMap {
                    case false => Redirect(routes.Auth.login).fuccess
                    case _ =>
                      val newUserEmail = userEmail.copy(email = EmailAddress(email))
                      EmailConfirmRateLimit(newUserEmail, ctx.req) {
                        lila.mon.email.send.fix.increment()
                        env.user.repo.setEmail(user.id, newUserEmail.email) >>
                          env.security.emailConfirm.send(user, newUserEmail.email) inject {
                            Redirect(routes.Auth.checkYourEmail) withCookies
                              lila.security.EmailConfirm.cookie
                                .make(env.lilaCookie, user, newUserEmail.email)(ctx.req)
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
          Redirect(routes.User.show(user.username)).fuccess
        case Result.AlreadyConfirmed(_) =>
          Redirect(routes.Auth.login).fuccess
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

  private def redirectNewUser(user: UserModel)(implicit ctx: Context) = {
    api.saveAuthentication(user.id, ctx.mobileApiVersion) flatMap { sessionId =>
      negotiate(
        html = Redirect(routes.User.show(user.username)).fuccess,
        api = _ => mobileUserOk(user, sessionId)
      ) map authenticateCookie(sessionId)
    } recoverWith authRecovery
  }

  def setFingerPrint(fp: String, ms: Int) =
    Auth { ctx => me =>
      lila.mon.http.fingerPrint.record(ms)
      api.setFingerPrint(ctx.req, FingerPrint(fp)) flatMap {
        _ ?? { hash =>
          !me.lame ?? (for {
            otherIds <- api.recentUserIdsByFingerHash(hash).map(_.filter(me.id.!=))
            _ <- (otherIds.sizeIs >= 2) ?? env.user.repo.countLameOrTroll(otherIds).flatMap {
              case nb if nb >= 2 && nb >= otherIds.size / 2 => env.report.api.autoAltPrintReport(me.id)
              case _                                        => funit
            }
          } yield ())
        }
      } inject NoContent
    }

  private def renderPasswordReset(form: Option[play.api.data.Form[PasswordReset]], fail: Boolean)(implicit
      ctx: Context
  ) =
    html.auth.bits.passwordReset(form.foldLeft(env.security.forms.passwordReset)(_ withForm _), fail)

  def passwordReset =
    Open { implicit ctx =>
      Ok(renderPasswordReset(none, fail = false)).fuccess
    }

  def passwordResetApply =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      env.security.hcaptcha.verify() flatMap { captcha =>
        if (captcha.ok)
          forms.passwordReset.form
            .bindFromRequest()
            .fold(
              err => BadRequest(renderPasswordReset(err.some, fail = true)).fuccess,
              data =>
                env.user.repo.enabledWithEmail(data.realEmail.normalize) flatMap {
                  case Some((user, storedEmail)) =>
                    lila.mon.user.auth.passwordResetRequest("success").increment()
                    env.security.passwordReset.send(user, storedEmail) inject Redirect(
                      routes.Auth.passwordResetSent(storedEmail.conceal)
                    )
                  case _ =>
                    lila.mon.user.auth.passwordResetRequest("noEmail").increment()
                    Redirect(routes.Auth.passwordResetSent(data.realEmail.conceal)).fuccess
                }
            )
        else BadRequest(renderPasswordReset(none, fail = true)).fuccess
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
          fuccess(html.auth.bits.passwordResetConfirm(user, token, forms.passwdReset, none))
      }
    }

  def passwordResetConfirmApply(token: String) =
    OpenBody { implicit ctx =>
      env.security.passwordReset confirm token flatMap {
        case None =>
          lila.mon.user.auth.passwordResetConfirm("tokenPostFail").increment()
          notFound
        case Some(user) =>
          implicit val req = ctx.body
          FormFuResult(forms.passwdReset) { err =>
            fuccess(html.auth.bits.passwordResetConfirm(user, token, err, false.some))
          } { data =>
            HasherRateLimit(user.username, ctx.req) { _ =>
              env.user.authenticator.setPassword(user.id, ClearPassword(data.newPasswd1)) >>
                env.user.repo.setEmailConfirmed(user.id).flatMap {
                  _ ?? { welcome(user, _, sendWelcomeEmail = false) }
                } >>
                env.user.repo.disableTwoFactor(user.id) >>
                env.security.store.closeAllSessionsOf(user.id) >>
                env.push.webSubscriptionApi.unsubscribeByUser(user) >>
                authenticateUser(user) >>-
                lila.mon.user.auth.passwordResetConfirm("success").increment().unit
            }(rateLimitedFu)
          }
      }
    }

  private def renderMagicLink(form: Option[play.api.data.Form[MagicLink]], fail: Boolean)(implicit
      ctx: Context
  ) =
    html.auth.bits.magicLink(form.foldLeft(env.security.forms.magicLink)(_ withForm _), fail)

  def magicLink =
    Open { implicit ctx =>
      Firewall {
        Ok(renderMagicLink(none, fail = false)).fuccess
      }
    }

  def magicLinkApply =
    OpenBody { implicit ctx =>
      Firewall {
        implicit val req = ctx.body
        env.security.hcaptcha.verify() flatMap { captcha =>
          if (captcha.ok)
            forms.magicLink.form
              .bindFromRequest()
              .fold(
                err => BadRequest(renderMagicLink(err.some, fail = true)).fuccess,
                data =>
                  env.user.repo.enabledWithEmail(data.realEmail.normalize)
                    flatMap {
                      case Some((user, storedEmail)) =>
                        MagicLinkRateLimit(user, storedEmail, ctx.req) {
                          lila.mon.user.auth.magicLinkRequest("success").increment()
                          env.security.magicLink.send(user, storedEmail) inject Redirect(
                            routes.Auth.magicLinkSent(storedEmail.value)
                          )
                        }(rateLimitedFu)
                      case _ =>
                        lila.mon.user.auth.magicLinkRequest("no_email").increment()
                        Redirect(routes.Auth.magicLinkSent(data.realEmail.value)).fuccess
                    }
              )
          else
            BadRequest(renderMagicLink(none, fail = true)).fuccess
        }
      }
    }

  def magicLinkSent(@nowarn("cat=unused") email: String) =
    Open { implicit ctx =>
      fuccess {
        Ok(html.auth.bits.magicLinkSent)
      }
    }

  private lazy val magicLinkLoginRateLimitPerToken = new RateLimit[String](
    credits = 3,
    duration = 1 hour,
    key = "login.magicLink.token"
  )

  def magicLinkLogin(token: String) =
    Open { implicit ctx =>
      Firewall {
        magicLinkLoginRateLimitPerToken(token) {
          env.security.magicLink confirm token flatMap {
            case None =>
              lila.mon.user.auth.magicLinkConfirm("token_fail").increment()
              notFound
            case Some(user) =>
              authLog(user.username, "-", "Magic link")
              authenticateUser(user) >>-
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
    AuthOrScoped(_.Web.Login)(_ => loginTokenFor, _ => loginTokenFor)

  def loginWithToken(token: String) =
    Open { implicit ctx =>
      if (ctx.isAuth) Redirect(getReferrer).fuccess
      else
        Firewall {
          consumingToken(token) { user =>
            env.security.loginToken.generate(user) map { newToken =>
              Ok(html.auth.bits.tokenLoginConfirmation(user, newToken, get("referrer")))
            }
          }
        }
    }

  def loginWithTokenPost(token: String, referrer: Option[String]) =
    Open { implicit ctx =>
      if (ctx.isAuth) Redirect(getReferrer).fuccess
      else
        Firewall {
          consumingToken(token) { authenticateUser(_) }
        }
    }

  private def consumingToken(token: String)(f: UserModel => Fu[Result])(implicit ctx: Context) =
    env.security.loginToken consume token flatMap {
      case None =>
        BadRequest {
          import scalatags.Text.all.stringFrag
          html.site.message("This token has expired.")(stringFrag("Please go back and try again."))
        }.fuccess
      case Some(user) => f(user)
    }

  implicit private val limitedDefault = Zero.instance[Result](rateLimited)

  private[controllers] def HasherRateLimit =
    PasswordHasher.rateLimit[Result](enforce = env.net.rateLimit) _

  private[controllers] def EmailConfirmRateLimit = lila.security.EmailConfirm.rateLimit[Result] _

  private[controllers] def MagicLinkRateLimit = lila.security.MagicLink.rateLimit[Result] _

  private[controllers] def RedirectToProfileIfLoggedIn(f: => Fu[Result])(implicit ctx: Context): Fu[Result] =
    ctx.me match {
      case Some(me) => Redirect(routes.User.show(me.username)).fuccess
      case None     => f
    }
}
