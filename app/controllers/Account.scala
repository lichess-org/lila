package controllers

import play.api.data.Form
import play.api.libs.json.*
import play.api.mvc.*
import views.account.pages

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.web.AnnounceApi

final class Account(
    env: Env,
    auth: Auth,
    apiC: => Api
) extends LilaController(env):

  def profile = Auth { _ ?=> me ?=>
    Ok.page:
      pages.profile(me, env.user.forms.profileOf(me))
  }

  def profileApply = AuthOrScopedBody(_.Web.Mobile) { _ ?=> me ?=>
    bindForm(env.user.forms.profile)(
      err =>
        negotiate(
          BadRequest.page(pages.profile(me, err)),
          jsonFormError(err)
        ),
      profile =>
        for
          _ <- profile.bio
            .exists(env.security.spam.detect)
            .option("profile.bio" -> ~profile.bio)
            .orElse:
              profile.links.exists(env.security.spam.detect).option("profile.links" -> ~profile.links)
            .so: (resource, text) =>
              env.report.api.autoCommFlag(lila.report.Suspect(me).id, resource, text)
          _ <- env.user.repo.setProfile(me, profile)
          _ <- bindForm(env.user.forms.flair)(_ => funit, env.user.repo.setFlair(me, _))
        yield
          env.user.lightUserApi.invalidate(me)
          Redirect(routes.User.show(me.userId)).flashSuccess
    )
  }

  def info = Auth { ctx ?=> me ?=>
    negotiateJson:
      for
        povs         <- env.round.proxyRepo.urgentGames(me)
        nbChallenges <- env.challenge.api.countInFor.get(me)
        playban      <- env.playban.api.currentBan(me)
        perfs        <- ctx.pref.showRatings.soFu(env.user.perfsRepo.perfsOf(me))
      yield Ok:
        env.user.jsonView
          .full(me, perfs, withProfile = false) ++ Json
          .obj(
            "prefs" -> lila.pref.JsonView.write(ctx.pref, lichobileCompat = HTTPRequest.isLichobile(req)),
            "nowPlaying"   -> JsArray(povs.take(50).map(env.api.lobbyApi.nowPlaying)),
            "nbChallenges" -> nbChallenges,
            "online"       -> true
          )
          .add("kid" -> ctx.kid)
          .add("troll" -> me.marks.troll)
          .add("playban" -> playban)
          .add("announce" -> AnnounceApi.get.map(_.json))
      .withHeaders(CACHE_CONTROL -> "max-age=15")
  }

  def nowPlaying = Auth { _ ?=> _ ?=>
    negotiateJson(doNowPlaying)
  }

  val apiMe = Scoped() { ctx ?=> me ?=>
    def limited = rateLimited:
      "Please don't poll this endpoint. Stream https://live.chess-online.com/api#tag/Board/operation/apiStreamEvent instead."
    val wikiGranted = getBool("wiki") && isGranted(_.LichessTeam) && ctx.scopes.has(_.Web.Mod)
    if getBool("wiki") && !wikiGranted then Unauthorized(jsonError("Wiki access not granted"))
    else
      limit.apiMe(me, limited):
        env.api.userApi
          .extended(
            me.value,
            withFollows = apiC.userWithFollows,
            withTrophies = false,
            withCanChallenge = false,
            withPlayban = getBool("playban"),
            forWiki = wikiGranted
          )
          .dmap { JsonOk(_) }
  }

  def apiNowPlaying = Scoped()(doNowPlaying)

  private def doNowPlaying(using ctx: Context)(using me: Me) =
    env.round.proxyRepo
      .urgentGames(me)
      .map:
        _.take((getInt("nb") | 9).atMost(50))
      .map:
        _.filterNot(_.game.isTournament).map(env.api.lobbyApi.nowPlaying)
      .map: povs =>
        Ok(Json.obj("nowPlaying" -> JsArray(povs)))

  def dasher = Auth { _ ?=> me ?=>
    negotiateJson:
      env.pref.api
        .get(me)
        .map: prefs =>
          Ok:
            lila.common.Json.lightUser.write(me.light) ++ Json.obj(
              "coach" -> isGranted(_.Coach),
              "prefs" -> lila.pref.JsonView.write(prefs, lichobileCompat = false)
            )
  }

  def apiEmail = Scoped(_.Email.Read) { _ ?=> me ?=>
    Found(env.user.repo.email(me)): email =>
      JsonOk(Json.obj("email" -> email.value))
  }

  def network(usingAltSocket: Option[Boolean]) = Auth { _ ?=> me ?=>
    val page = (use: Option[Boolean]) => Ok.page(pages.network(use, ctx.pref.isUsingAltSocket))
    if usingAltSocket.isEmpty || usingAltSocket.has(ctx.pref.isUsingAltSocket) then page(none)
    else env.pref.api.setPref(me, ctx.pref.copy(usingAltSocket = usingAltSocket)) >> page(usingAltSocket)
  }

  private def refreshSessionId(result: Result)(using ctx: Context, me: Me): Fu[Result] = for
    _         <- env.security.store.closeAllSessionsOf(me)
    _         <- env.push.webSubscriptionApi.unsubscribeByUser(me)
    _         <- env.push.unregisterDevices(me)
    sessionId <- env.security.api.saveAuthentication(me, ctx.mobileApiVersion)
  yield result.withCookies(env.security.lilaCookie.session(env.security.api.sessionIdKey, sessionId))

  def close = Auth { _ ?=> me ?=>
    for
      managed <- env.clas.api.student.isManaged(me)
      form = env.security.forms.closeAccount
      res <- Ok.page(pages.close(form, managed))
    yield res
  }

  def closeConfirm = AuthBody { ctx ?=> me ?=>
    NotManaged:
      env.mod.logApi.closedByMod(me.value).flatMap {
        case true => notFound
        case false =>
          limit.closeAccount(me.userId, rateLimited):
            val form = env.security.forms.closeAccount
            FormFuResult(form)(err => renderPage(pages.close(err, managed = false))): forever =>
              for _ <- env.api.accountTermination.disable(me.value, forever = forever)
              yield Redirect(routes.Lobby.home).withCookies(env.security.lilaCookie.newSession)
      }
  }

  def kid = Auth { _ ?=> me ?=>
    for
      managed <- env.clas.api.student.isManaged(me)
      form = env.security.forms.toggleKid
      page <- Ok.page(pages.kid(me, form, managed))
    yield page
  }
  def apiKid = Scoped(_.Preference.Read) { _ ?=> me ?=>
    JsonOk(Json.obj("kid" -> me.kid))
  }

  def kidPost = AuthBody { ctx ?=> me ?=>
    NotManaged:
      limit.kidMode(me.userId, rateLimited):
        val form = env.security.forms.toggleKid
        bindForm(form)(
          err =>
            negotiate(
              BadRequest.page(pages.kid(me, err, managed = false)),
              BadRequest(errorsAsJson(err))
            ),
          _ =>
            for
              _ <- env.user.repo.setKid(me, getBool("v"))
              res <- negotiate(
                Redirect(routes.Account.kid).flashSuccess,
                jsonOkResult
              )
            yield res
        )
  }

  def apiKidPost = Scoped(_.Preference.Write) { ctx ?=> me ?=>
    getBoolOpt("v") match
      case None    => BadRequest(jsonError("Missing v parameter"))
      case Some(v) => env.user.repo.setKid(me, v).inject(jsonOkResult)
  }

  def security = Auth { _ ?=> me ?=>
    for
      _                    <- env.security.api.dedup(me, req)
      sessions             <- env.security.api.locatedOpenSessions(me, 50)
      clients              <- env.oAuth.tokenApi.listClients(50)
      personalAccessTokens <- env.oAuth.tokenApi.countPersonal
      currentSessionId = ~env.security.api.reqSessionId(req)
      page <- Ok.async:
        views.account.security(me, sessions, currentSessionId, clients, personalAccessTokens)
    yield page
  }

  def signout(sessionId: String) = Auth { _ ?=> me ?=>
    if sessionId == "all"
    then refreshSessionId(Redirect(routes.Account.security).flashSuccess)
    else
      for
        _ <- env.security.store.closeUserAndSessionId(me, sessionId)
        _ <- env.push.webSubscriptionApi.unsubscribeBySession(sessionId)
      yield NoContent
  }

  private def renderReopen(form: Option[Form[Boolean]], msg: Option[String])(using Context)(using Me) =
    pages.reopen.form(form.getOrElse(env.security.forms.reopenConfirm), msg)

  def reopen = Open:
    ctx.me match
      case Some(me) if ctx.isAppealUser => Ok.async(renderReopen(none, none)(using ctx)(using me))
      case Some(me)                     => Redirect(routes.User.show(me.userId))
      case None                         => Redirect(routes.Auth.login)

  def reopenApply = AuthBody { ctx ?=> me ?=>
    if !ctx.isAppealUser then notFound
    else
      limit.reopen(me.userId, rateLimited):
        bindForm(env.security.forms.reopenConfirm)(
          err => BadRequest.async(renderReopen(err.some, none)),
          _ => reopenAccount(me)
        )
  }

  private def reopenAccount(me: Me)(using Context): Fu[Result] =
    for
      foreverClosed <- env.user.repo.isForeverClosed(me.value)
      deleted       <- env.user.repo.isDeleted(me.value)
      closedByMod   <- env.mod.logApi.closedByMod(me.value)
      result <-
        if foreverClosed || deleted || closedByMod then notFound
        else
          for
            _ <- env.user.repo.reopen(me.userId)
            _ = lila.log("auth").info(s"Reopen by ${me.userId} ${HTTPRequest.printClient(ctx.req)}")
            _ = lila.mon.user.auth.reopenConfirm("success").increment()
            _ <- env.report.api.reopenReports(lila.report.Suspect(me.value))
            _ = env.security.api.reqSessionId(ctx.req).so(env.security.api.appeal.delete)
            user <- env.user.repo.byId(me.userId).orFail("Reopened user not found")
            res  <- auth.authenticateUser(user, remember = true)
          yield res
    yield result

  def data = Auth { _ ?=> me ?=>
    meOrFetch(getUserStr("user"))
      .map:
        _.filter(u => ctx.is(u) || isGrantedOpt(_.Impersonate))
      .orNotFound: user =>
        if getBool("text") then
          apiC.GlobalConcurrencyLimitUser(me)(env.api.personalDataExport(user)): source =>
            Ok.chunked(source.map(_ + "\n"))
              .pipe(asAttachmentStream(s"chess_${user.username}.txt"))
        else Ok.page(pages.data(user))
  }
