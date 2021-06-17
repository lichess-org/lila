package controllers

import play.api.libs.json._
import play.api.mvc._
import scala.annotation.nowarn
import scala.util.chaining._
import views.html

import lila.api.AnnounceStore
import lila.api.Context
import lila.app._
import lila.common.HTTPRequest
import lila.security.SecurityForm.Reopen
import lila.user.{ User => UserModel, TotpSecret, Holder }

final class Account(
    env: Env,
    auth: Auth,
    apiC: => Api
) extends LilaController(env) {

  def profile =
    Auth { implicit ctx => me =>
      Ok(html.account.profile(me, env.user.forms profileOf me)).fuccess
    }

  def username =
    Auth { implicit ctx => me =>
      Ok(html.account.username(me, env.user.forms usernameOf me)).fuccess
    }

  def profileApply =
    AuthBody { implicit ctx => me =>
      implicit val req: Request[_] = ctx.body
      FormFuResult(env.user.forms.profile) { err =>
        fuccess(html.account.profile(me, err))
      } { profile =>
        profile.bio
          .exists(env.security.spam.detect)
          .option("profile.bio" -> ~profile.bio)
          .orElse {
            profile.links
              .exists(env.security.spam.detect)
              .option("profile.links" -> ~profile.links)
          }
          .?? { case (resource, text) =>
            env.report.api.autoCommFlag(lila.report.Suspect(me).id, resource, text)
          } >> env.user.repo.setProfile(me.id, profile) inject Redirect(routes.Account.profile).flashSuccess
      }
    }

  def usernameApply =
    AuthBody { implicit ctx => me =>
      implicit val req: Request[_] = ctx.body
      FormFuResult(env.user.forms.username(me)) { err =>
        fuccess(html.account.username(me, err))
      } { username =>
        env.user.repo
          .setUsernameCased(me.id, username) inject
          Redirect(routes.User show me.username).flashSuccess recover { case e =>
            BadRequest(html.account.username(me, env.user.forms.username(me))).flashFailure(e.getMessage)
          }
      }
    }

  def info =
    Auth { implicit ctx => me =>
      negotiate(
        html = notFound,
        api = _ => {
          env.relation.api.countFollowers(me.id) zip
            env.pref.api.getPref(me) zip
            env.round.proxyRepo.urgentGames(me) zip
            env.challenge.api.countInFor.get(me.id) zip
            env.playban.api.currentBan(me.id) map {
              case ((((nbFollowers, prefs), povs), nbChallenges), playban) =>
                Ok {
                  import lila.pref.JsonView._
                  env.user.jsonView(me) ++ Json
                    .obj(
                      "prefs"        -> prefs,
                      "nowPlaying"   -> JsArray(povs take 50 map env.api.lobbyApi.nowPlaying),
                      "nbFollowers"  -> nbFollowers,
                      "nbChallenges" -> nbChallenges
                    )
                    .add("kid" -> me.kid)
                    .add("troll" -> me.marks.troll)
                    .add("playban" -> playban)
                    .add("announce" -> AnnounceStore.get.map(_.json))
                }.withHeaders(CACHE_CONTROL -> s"max-age=15")
            }
        }
      )
    }

  def nowPlaying =
    Auth { implicit ctx => me =>
      negotiate(
        html = notFound,
        api = _ => doNowPlaying(me, ctx.req)
      )
    }

  def apiMe =
    Scoped() { _ => me =>
      env.api.userApi.extended(me, me.some) dmap { JsonOk(_) }
    }

  def apiNowPlaying =
    Scoped() { req => me =>
      doNowPlaying(me, req)
    }

  private def doNowPlaying(me: lila.user.User, req: RequestHeader) =
    env.round.proxyRepo.urgentGames(me) map { povs =>
      val nb = (getInt("nb", req) | 9) atMost 50
      Ok(Json.obj("nowPlaying" -> JsArray(povs take nb map env.api.lobbyApi.nowPlaying)))
    }

  def dasher =
    Auth { implicit ctx => me =>
      negotiate(
        html = notFound,
        api = _ =>
          env.pref.api.getPref(me) map { prefs =>
            Ok {
              import lila.pref.JsonView._
              lila.common.LightUser.lightUserWrites.writes(me.light) ++ Json.obj(
                "coach" -> isGranted(_.Coach),
                "prefs" -> prefs
              )
            }
          }
      )
    }

  def passwd =
    Auth { implicit ctx => me =>
      env.user.forms passwd me map { form =>
        Ok(html.account.passwd(form))
      }
    }

  def passwdApply =
    AuthBody { implicit ctx => me =>
      auth.HasherRateLimit(me.username, ctx.req) { _ =>
        implicit val req = ctx.body
        env.user.forms passwd me flatMap { form =>
          FormFuResult(form) { err =>
            fuccess(html.account.passwd(err))
          } { data =>
            env.user.authenticator.setPassword(me.id, UserModel.ClearPassword(data.newPasswd1)) >>
              refreshSessionId(me, Redirect(routes.Account.passwd).flashSuccess)
          }
        }
      }(rateLimitedFu)
    }

  private def refreshSessionId(me: UserModel, result: Result)(implicit ctx: Context): Fu[Result] =
    env.security.store.closeAllSessionsOf(me.id) >>
      env.push.webSubscriptionApi.unsubscribeByUser(me) >>
      env.security.api.saveAuthentication(me.id, ctx.mobileApiVersion) map { sessionId =>
        result.withCookies(env.lilaCookie.session(env.security.api.sessionIdKey, sessionId))
      }

  private def emailForm(user: UserModel) =
    env.user.repo email user.id flatMap {
      env.security.forms.changeEmail(user, _)
    }

  def email =
    Auth { implicit ctx => me =>
      if (getBool("check")) Ok(renderCheckYourEmail).fuccess
      else
        emailForm(me) map { form =>
          Ok(html.account.email(form))
        }
    }

  def apiEmail =
    Scoped(_.Email.Read) { _ => me =>
      env.user.repo email me.id map {
        _ ?? { email =>
          JsonOk(Json.obj("email" -> email.value))
        }
      }
    }

  def renderCheckYourEmail(implicit ctx: Context) =
    html.auth.checkYourEmail(lila.security.EmailConfirm.cookie get ctx.req)

  def emailApply =
    AuthBody { implicit ctx => me =>
      auth.HasherRateLimit(me.username, ctx.req) { _ =>
        implicit val req = ctx.body
        env.security.forms.preloadEmailDns >> emailForm(me).flatMap { form =>
          FormFuResult(form) { err =>
            fuccess(html.account.email(err))
          } { data =>
            val email = env.security.emailAddressValidator
              .validate(data.realEmail) err s"Invalid email ${data.email}"
            val newUserEmail = lila.security.EmailConfirm.UserEmail(me.username, email.acceptable)
            auth.EmailConfirmRateLimit(newUserEmail, ctx.req) {
              env.security.emailChange.send(me, newUserEmail.email) inject
                Redirect(routes.Account.email).flashSuccess {
                  lila.i18n.I18nKeys.checkYourEmail.txt()
                }
            }(rateLimitedFu)
          }
        }
      }(rateLimitedFu)
    }

  def emailConfirm(token: String) =
    Open { implicit ctx =>
      env.security.emailChange.confirm(token) flatMap {
        _ ?? { case (user, prevEmail) =>
          (prevEmail.exists(_.isNoReply) ?? env.clas.api.student.release(user)) >>
            auth.authenticateUser(
              user,
              result =
                if (prevEmail.exists(_.isNoReply))
                  Some(_ => Redirect(routes.User.show(user.username)).flashSuccess)
                else
                  Some(_ => Redirect(routes.Account.email).flashSuccess)
            )
        }
      }
    }

  def emailConfirmHelp =
    OpenBody { implicit ctx =>
      import lila.security.EmailConfirm.Help._
      ctx.me match {
        case Some(me) =>
          Redirect(routes.User.show(me.username)).fuccess
        case None if get("username").isEmpty =>
          Ok(html.account.emailConfirmHelp(helpForm, none)).fuccess
        case None =>
          implicit val req = ctx.body
          helpForm
            .bindFromRequest()
            .fold(
              err => BadRequest(html.account.emailConfirmHelp(err, none)).fuccess,
              username =>
                getStatus(env.user.repo, username) map { status =>
                  Ok(html.account.emailConfirmHelp(helpForm fill username, status.some))
                }
            )
      }
    }

  def twoFactor =
    Auth { implicit ctx => me =>
      if (me.totpSecret.isDefined)
        env.security.forms.disableTwoFactor(me) map { form =>
          html.account.twoFactor.disable(me, form)
        }
      else
        env.security.forms.setupTwoFactor(me) map { form =>
          html.account.twoFactor.setup(me, form)
        }
    }

  def setupTwoFactor =
    AuthBody { implicit ctx => me =>
      auth.HasherRateLimit(me.username, ctx.req) { _ =>
        implicit val req = ctx.body
        env.security.forms.setupTwoFactor(me) flatMap { form =>
          FormFuResult(form) { err =>
            fuccess(html.account.twoFactor.setup(me, err))
          } { data =>
            env.user.repo.setupTwoFactor(me.id, TotpSecret(data.secret)) >>
              refreshSessionId(me, Redirect(routes.Account.twoFactor).flashSuccess)
          }
        }
      }(rateLimitedFu)
    }

  def disableTwoFactor =
    AuthBody { implicit ctx => me =>
      auth.HasherRateLimit(me.username, ctx.req) { _ =>
        implicit val req = ctx.body
        env.security.forms.disableTwoFactor(me) flatMap { form =>
          FormFuResult(form) { err =>
            fuccess(html.account.twoFactor.disable(me, err))
          } { _ =>
            env.user.repo.disableTwoFactor(me.id) inject
              Redirect(routes.Account.twoFactor).flashSuccess
          }
        }
      }(rateLimitedFu)
    }

  def close =
    Auth { implicit ctx => me =>
      env.clas.api.student.isManaged(me) flatMap { managed =>
        env.security.forms closeAccount me map { form =>
          Ok(html.account.close(me, form, managed))
        }
      }
    }

  def closeConfirm =
    AuthBody { implicit ctx => me =>
      NotManaged {
        implicit val req = ctx.body
        env.security.forms closeAccount me flatMap { form =>
          FormFuResult(form) { err =>
            fuccess(html.account.close(me, err, managed = false))
          } { _ =>
            env.closeAccount(me, Holder(me)) inject {
              Redirect(routes.User show me.username) withCookies env.lilaCookie.newSession
            }
          }
        }
      }
    }

  def kid =
    Auth { implicit ctx => me =>
      env.clas.api.student.isManaged(me) flatMap { managed =>
        env.security.forms toggleKid me map { form =>
          Ok(html.account.kid(me, form, managed))
        }
      }
    }
  def apiKid =
    Scoped(_.Preference.Read) { _ => me =>
      JsonOk(Json.obj("kid" -> me.kid)).fuccess
    }

  def kidPost =
    AuthBody { implicit ctx => me =>
      NotManaged {
        implicit val req = ctx.body
        env.security.forms toggleKid me flatMap { form =>
          form
            .bindFromRequest()
            .fold(
              err =>
                negotiate(
                  html = BadRequest(html.account.kid(me, err, managed = false)).fuccess,
                  api = _ => BadRequest(errorsAsJson(err)).fuccess
                ),
              _ =>
                env.user.repo.setKid(me, getBool("v")) >>
                  negotiate(
                    html = Redirect(routes.Account.kid).flashSuccess.fuccess,
                    api = _ => jsonOkResult.fuccess
                  )
            )
        }
      }
    }

  def apiKidPost =
    Scoped(_.Preference.Write) { req => me =>
      env.user.repo.setKid(me, getBool("v", req)) inject jsonOkResult
    }

  private def currentSessionId(implicit ctx: Context) =
    ~env.security.api.reqSessionId(ctx.req)

  def security =
    Auth { implicit ctx => me =>
      env.security.api.dedup(me.id, ctx.req) >>
        env.security.api.locatedOpenSessions(me.id, 50) map { sessions =>
          Ok(
            html.account
              .security(me, sessions, currentSessionId, thirdPartyApps = true, personalAccessTokens = true)
          )
        }
    }

  def signout(sessionId: String) =
    Auth { implicit _ctx => me =>
      if (sessionId == "all")
        refreshSessionId(me, Redirect(routes.Account.security).flashSuccess)
      else
        env.security.store.closeUserAndSessionId(me.id, sessionId) >>
          env.push.webSubscriptionApi.unsubscribeBySession(sessionId)
    }

  private def renderReopen(form: Option[play.api.data.Form[Reopen]], msg: Option[String])(implicit
      ctx: Context
  ) =
    html.account.reopen.form(form.foldLeft(env.security.forms.reopen)(_ withForm _), msg)

  def reopen =
    Open { implicit ctx =>
      auth.RedirectToProfileIfLoggedIn {
        Ok(renderReopen(none, none)).fuccess
      }
    }

  def reopenApply =
    OpenBody { implicit ctx =>
      implicit val req = ctx.body
      env.security.hcaptcha.verify() flatMap { captcha =>
        if (captcha.ok)
          env.security.forms.reopen.form
            .bindFromRequest()
            .fold(
              err => BadRequest(renderReopen(err.some, none)).fuccess,
              data =>
                env.security.reopen
                  .prepare(data.username, data.realEmail, env.mod.logApi.closedByMod) flatMap {
                  case Left((code, msg)) =>
                    lila.mon.user.auth.reopenRequest(code).increment()
                    BadRequest(renderReopen(none, msg.some)).fuccess
                  case Right(user) =>
                    auth.MagicLinkRateLimit(user, data.realEmail, ctx.req) {
                      lila.mon.user.auth.reopenRequest("success").increment()
                      env.security.reopen.send(user, data.realEmail) inject Redirect(
                        routes.Account.reopenSent(data.realEmail.value)
                      )
                    }(rateLimitedFu)
                }
            )
        else BadRequest(renderReopen(none, none)).fuccess
      }
    }

  def reopenSent(@nowarn("cat=unused") email: String) =
    Open { implicit ctx =>
      fuccess {
        Ok(html.account.reopen.sent)
      }
    }

  def reopenLogin(token: String) =
    Open { implicit ctx =>
      env.security.reopen confirm token flatMap {
        case None =>
          lila.mon.user.auth.reopenConfirm("token_fail").increment()
          notFound
        case Some(user) =>
          env.report.api.reopenReports(lila.report.Suspect(user)) >>
            auth.authenticateUser(user) >>-
            lila.mon.user.auth.reopenConfirm("success").increment().unit
      }
    }

  def data =
    Auth { implicit ctx => me =>
      val userId = get("user")
        .map(lila.user.User.normalize)
        .filter(id => me.id == id || isGranted(_.Impersonate)) | me.id
      env.user.repo byId userId map {
        _ ?? { user =>
          if (getBool("text"))
            apiC.GlobalConcurrencyLimitUser(me.id)(
              env.api.personalDataExport(user)
            ) { source =>
              Ok.chunked(source.map(_ + "\n"))
                .pipe(asAttachmentStream(s"lichess_${user.username}.txt"))
            }
          else Ok(html.account.bits.data(user))
        }
      }
    }
}
