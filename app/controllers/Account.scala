package controllers

import play.api.libs.json.*
import play.api.mvc.*
import scala.util.chaining.*
import views.html

import lila.api.AnnounceStore
import lila.api.Context
import lila.app.{ given, * }
import lila.security.SecurityForm.Reopen
import lila.user.{ Holder, TotpSecret, User as UserModel }
import lila.i18n.I18nLangPicker

final class Account(
    env: Env,
    auth: Auth,
    apiC: => Api
) extends LilaController(env):

  def profile = Auth { _ ?=> me =>
    Ok(html.account.profile(me, env.user.forms profileOf me)).toFuccess
  }

  def username = Auth { _ ?=> me =>
    Ok(html.account.username(me, env.user.forms usernameOf me)).toFuccess
  }

  def profileApply = AuthBody { _ ?=> me =>
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
        } >> env.user.repo.setProfile(me.id, profile) inject
        Redirect(routes.User show me.username).flashSuccess
    }
  }

  def usernameApply = AuthBody { _ ?=> me =>
    FormFuResult(env.user.forms.username(me)) { err =>
      fuccess(html.account.username(me, err))
    } { username =>
      env.user.repo
        .setUsernameCased(me.id, username) inject
        Redirect(routes.User show me.username).flashSuccess recover { case e =>
          Redirect(routes.Account.username).flashFailure(e.getMessage)
        }
    }
  }

  def info = Auth { _ ?=> me =>
    negotiate(
      html = notFound,
      api = _ =>
        for {
          povs         <- env.round.proxyRepo urgentGames me
          nbChallenges <- env.challenge.api.countInFor get me.id
          playban      <- env.playban.api currentBan me.id
        } yield Ok {
          import lila.pref.JsonView.given
          env.user.jsonView
            .full(me, withRating = ctx.pref.showRatings, withProfile = false) ++ Json
            .obj(
              "prefs"        -> ctx.pref,
              "nowPlaying"   -> JsArray(povs take 50 map env.api.lobbyApi.nowPlaying),
              "nbChallenges" -> nbChallenges,
              "online"       -> true
            )
            .add("kid" -> me.kid)
            .add("troll" -> me.marks.troll)
            .add("playban" -> playban)
            .add("announce" -> AnnounceStore.get.map(_.json))
        }.withHeaders(CACHE_CONTROL -> "max-age=15")
    )
  }

  def nowPlaying = Auth { _ ?=> me =>
    negotiate(
      html = notFound,
      api = _ => doNowPlaying(me, ctx.req)
    )
  }

  val apiMe =
    val rateLimit = lila.memo.RateLimit[UserId](30, 10.minutes, "api.account.user")
    Scoped() { req ?=> me =>
      def limited = rateLimitedFu:
        "Please don't poll this endpoint. Stream https://lichess.org/api#tag/Board/operation/apiStreamEvent instead."
      rateLimit(me.id, limited):
        env.api.userApi.extended(
          me,
          me.some,
          withFollows = apiC.userWithFollows(req),
          withTrophies = false
        )(using reqLang(me)) dmap { JsonOk(_) }
    }

  def apiNowPlaying = Scoped() { req ?=> me =>
    doNowPlaying(me, req)
  }

  private def doNowPlaying(me: lila.user.User, req: RequestHeader) =
    env.round.proxyRepo.urgentGames(me) map { povs =>
      val nb = (getInt("nb", req) | 9) atMost 50
      Ok(Json.obj("nowPlaying" -> JsArray(povs take nb map env.api.lobbyApi.nowPlaying)))
    }

  def dasher = Auth { _ ?=> me =>
    negotiate(
      html = notFound,
      api = _ =>
        env.pref.api.getPref(me) map { prefs =>
          Ok {
            import lila.pref.JsonView.given
            lila.common.LightUser.lightUserWrites.writes(me.light) ++ Json.obj(
              "coach" -> isGranted(_.Coach),
              "prefs" -> prefs
            )
          }
        }
    )
  }

  def passwd = Auth { _ ?=> me =>
    env.security.forms passwdChange me map { form =>
      Ok(html.account.passwd(form))
    }
  }

  def passwdApply = AuthBody { ctx ?=> me =>
    auth.HasherRateLimit(me.id, ctx.req) {
      env.security.forms passwdChange me flatMap { form =>
        FormFuResult(form) { err =>
          fuccess(html.account.passwd(err))
        } { data =>
          env.user.authenticator.setPassword(me.id, UserModel.ClearPassword(data.newPasswd1)) >>
            refreshSessionId(me, Redirect(routes.Account.passwd).flashSuccess)
        }
      }
    }
  }

  private def refreshSessionId(me: UserModel, result: Result)(using ctx: Context): Fu[Result] =
    env.security.store.closeAllSessionsOf(me.id) >>
      env.push.webSubscriptionApi.unsubscribeByUser(me) >>
      env.push.unregisterDevices(me) >>
      env.security.api.saveAuthentication(me.id, ctx.mobileApiVersion) map { sessionId =>
        result.withCookies(env.lilaCookie.session(env.security.api.sessionIdKey, sessionId))
      }

  private def emailForm(user: UserModel) =
    env.user.repo email user.id flatMap {
      env.security.forms.changeEmail(user, _)
    }

  def email = Auth { _ ?=> me =>
    if getBool("check")
    then Ok(renderCheckYourEmail).toFuccess
    else
      emailForm(me).map: form =>
        Ok(html.account.email(form))
  }

  def apiEmail = Scoped(_.Email.Read) { _ ?=> me =>
    env.user.repo email me.id mapz { email =>
      JsonOk(Json.obj("email" -> email.value))
    }
  }

  def renderCheckYourEmail(using Context) =
    html.auth.checkYourEmail(lila.security.EmailConfirm.cookie get ctx.req)

  def emailApply = AuthBody { ctx ?=> me =>
    auth.HasherRateLimit(me.id, ctx.req):
      env.security.forms.preloadEmailDns() >> emailForm(me).flatMap { form =>
        FormFuResult(form) { err =>
          fuccess(html.account.email(err))
        } { data =>
          val newUserEmail = lila.security.EmailConfirm.UserEmail(me.username, data.email)
          auth.EmailConfirmRateLimit(newUserEmail, ctx.req, rateLimitedFu):
            env.security.emailChange.send(me, newUserEmail.email) inject
              Redirect(routes.Account.email).flashSuccess:
                lila.i18n.I18nKeys.checkYourEmail.txt()
        }
      }
  }

  def emailConfirm(token: String) = Open:
    env.security.emailChange.confirm(token) flatMapz { (user, prevEmail) =>
      (prevEmail.exists(_.isNoReply) ?? env.clas.api.student.release(user)) >>
        auth.authenticateUser(
          user,
          remember = true,
          result =
            if (prevEmail.exists(_.isNoReply))
              Some(_ => Redirect(routes.User.show(user.username)).flashSuccess)
            else
              Some(_ => Redirect(routes.Account.email).flashSuccess)
        )
    }

  def emailConfirmHelp = OpenBody:
    import lila.security.EmailConfirm.Help.*
    ctx.me match
      case Some(me) =>
        Redirect(routes.User.show(me.username)).toFuccess
      case None if get("username").isEmpty =>
        Ok(html.account.emailConfirmHelp(helpForm, none)).toFuccess
      case None =>
        helpForm
          .bindFromRequest()
          .fold(
            err => BadRequest(html.account.emailConfirmHelp(err, none)).toFuccess,
            username =>
              getStatus(env.user.repo, username) map { status =>
                Ok(html.account.emailConfirmHelp(helpForm fill username, status.some))
              }
          )

  def twoFactor = Auth { _ ?=> me =>
    if (me.totpSecret.isDefined)
      env.security.forms.disableTwoFactor(me) map { form =>
        html.account.twoFactor.disable(me, form)
      }
    else
      env.security.forms.setupTwoFactor(me) map { form =>
        html.account.twoFactor.setup(me, form)
      }
  }

  def setupTwoFactor = AuthBody { ctx ?=> me =>
    auth.HasherRateLimit(me.id, ctx.req):
      env.security.forms.setupTwoFactor(me) flatMap { form =>
        FormFuResult(form) { err =>
          fuccess(html.account.twoFactor.setup(me, err))
        } { data =>
          env.user.repo.setupTwoFactor(me.id, TotpSecret(data.secret)) >>
            refreshSessionId(me, Redirect(routes.Account.twoFactor).flashSuccess)
        }
      }
  }

  def disableTwoFactor = AuthBody { ctx ?=> me =>
    auth.HasherRateLimit(me.id, ctx.req):
      env.security.forms.disableTwoFactor(me) flatMap { form =>
        FormFuResult(form) { err =>
          fuccess(html.account.twoFactor.disable(me, err))
        } { _ =>
          env.user.repo.disableTwoFactor(me.id) inject
            Redirect(routes.Account.twoFactor).flashSuccess
        }
      }
  }

  def close = Auth { _ ?=> me =>
    env.clas.api.student.isManaged(me) flatMap { managed =>
      env.security.forms closeAccount me map { form =>
        Ok(html.account.close(me, form, managed))
      }
    }
  }

  def closeConfirm = AuthBody { ctx ?=> me =>
    NotManaged:
      auth.HasherRateLimit(me.id, ctx.req):
        env.security.forms closeAccount me flatMap { form =>
          FormFuResult(form) { err =>
            fuccess(html.account.close(me, err, managed = false))
          } { _ =>
            env.api.accountClosure
              .close(me, Holder(me))
              .inject:
                Redirect(routes.User show me.username) withCookies env.lilaCookie.newSession
          }
        }
  }

  def kid = Auth { _ ?=> me =>
    env.clas.api.student.isManaged(me) flatMap { managed =>
      env.security.forms toggleKid me map { form =>
        Ok(html.account.kid(me, form, managed))
      }
    }
  }
  def apiKid = Scoped(_.Preference.Read) { _ ?=> me =>
    JsonOk(Json.obj("kid" -> me.kid)).toFuccess
  }

  def kidPost = AuthBody { ctx ?=> me =>
    NotManaged:
      env.security.forms toggleKid me flatMap { form =>
        form
          .bindFromRequest()
          .fold(
            err =>
              negotiate(
                html = BadRequest(html.account.kid(me, err, managed = false)).toFuccess,
                api = _ => BadRequest(errorsAsJson(err)).toFuccess
              ),
            _ =>
              env.user.repo.setKid(me, getBool("v")) >>
                negotiate(
                  html = Redirect(routes.Account.kid).flashSuccess.toFuccess,
                  api = _ => jsonOkResult.toFuccess
                )
          )
      }
  }

  def apiKidPost = Scoped(_.Preference.Write) { req ?=> me =>
    getBoolOpt("v", req) match
      case None    => BadRequest(jsonError("Missing v parameter")).toFuccess
      case Some(v) => env.user.repo.setKid(me, v) inject jsonOkResult
  }

  private def currentSessionId(using Context) =
    ~env.security.api.reqSessionId(ctx.req)

  def security = Auth { _ ?=> me =>
    for
      _                    <- env.security.api.dedup(me.id, ctx.req)
      sessions             <- env.security.api.locatedOpenSessions(me.id, 50)
      clients              <- env.oAuth.tokenApi.listClients(me, 50)
      personalAccessTokens <- env.oAuth.tokenApi.countPersonal(me)
    yield Ok(
      html.account
        .security(
          me,
          sessions,
          currentSessionId,
          clients,
          personalAccessTokens
        )
    )
  }

  def signout(sessionId: String) = Auth { _ ?=> me =>
    if (sessionId == "all")
      refreshSessionId(me, Redirect(routes.Account.security).flashSuccess)
    else
      env.security.store.closeUserAndSessionId(me.id, sessionId) >>
        env.push.webSubscriptionApi.unsubscribeBySession(sessionId)
  }

  private def renderReopen(form: Option[play.api.data.Form[Reopen]], msg: Option[String])(using
      ctx: Context
  ) =
    env.security.forms.reopen map { baseForm =>
      html.account.reopen.form(form.foldLeft(baseForm)(_ withForm _), msg)
    }

  def reopen = Open:
    auth.RedirectToProfileIfLoggedIn:
      renderReopen(none, none) map { Ok(_) }

  def reopenApply = OpenBody:
    env.security.hcaptcha.verify() flatMap { captcha =>
      if (captcha.ok)
        env.security.forms.reopen flatMap {
          _.form
            .bindFromRequest()
            .fold(
              err => renderReopen(err.some, none) map { BadRequest(_) },
              data =>
                env.security.reopen
                  .prepare(data.username, data.email, env.mod.logApi.closedByMod) flatMap {
                  case Left((code, msg)) =>
                    lila.mon.user.auth.reopenRequest(code).increment()
                    renderReopen(none, msg.some) map { BadRequest(_) }
                  case Right(user) =>
                    auth.MagicLinkRateLimit(user, data.email, ctx.req, rateLimitedFu):
                      lila.mon.user.auth.reopenRequest("success").increment()
                      env.security.reopen.send(user, data.email) inject Redirect:
                        routes.Account.reopenSent
                }
            )
        }
      else renderReopen(none, none) map { BadRequest(_) }
    }

  def reopenSent = Open:
    fuccess:
      Ok(html.account.reopen.sent)

  def reopenLogin(token: String) = Open:
    env.security.reopen confirm token flatMap {
      case None =>
        lila.mon.user.auth.reopenConfirm("token_fail").increment()
        notFound
      case Some(user) =>
        env.report.api.reopenReports(lila.report.Suspect(user)) >>
          auth.authenticateUser(user, remember = true) >>-
          lila.mon.user.auth.reopenConfirm("success").increment().unit
    }

  def data = Auth { _ ?=> me =>
    val userId: UserId = getUserStr("user")
      .map(_.id)
      .filter(id => me == id || isGranted(_.Impersonate)) | me.id
    env.user.repo byId userId mapz { user =>
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
