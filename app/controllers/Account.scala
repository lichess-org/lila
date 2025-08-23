package controllers

import play.api.data.Form
import play.api.libs.json.*
import play.api.mvc.*
import views.account.pages

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.core.id.SessionId
import lila.security.SecurityForm.Reopen
import lila.web.AnnounceApi
import lila.core.user.KidMode
import lila.security.IsPwned
import lila.core.security.ClearPassword

final class Account(
    env: Env,
    auth: Auth,
    apiC: => Api
) extends LilaController(env):

  def profile = Auth { _ ?=> me ?=>
    Ok.page:
      pages.profile(me, env.user.forms.profileOf(me))
  }

  def username = Auth { _ ?=> me ?=>
    Ok.page:
      pages.username(me, env.user.forms.usernameOf(me))
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
          flairForm = env.user.forms.flair(asMod = isGranted(_.LichessTeam))
          _ <- bindForm(flairForm)(_ => funit, env.user.repo.setFlair(me, _))
        yield
          env.user.lightUserApi.invalidate(me)
          Redirect(routes.User.show(me.username)).flashSuccess
    )
  }

  def usernameApply = AuthBody { _ ?=> me ?=>
    FormFuResult(env.user.forms.username(me))(err => renderPage(pages.username(me, err))): username =>
      env.user.repo
        .setUsernameCased(me, username)
        .inject(Redirect(routes.User.show(me.username)).flashSuccess)
        .recover: e =>
          Redirect(routes.Account.username).flashFailure(e.getMessage)
  }

  def info = Auth { ctx ?=> me ?=>
    negotiateJson:
      for
        povs <- env.round.proxyRepo.urgentGames(me)
        nbChallenges <- env.challenge.api.countInFor.get(me)
        playban <- env.playban.api.currentBan(me)
        perfs <- ctx.pref.showRatings.optionFu(env.user.perfsRepo.perfsOf(me))
      yield Ok:
        env.user.jsonView
          .full(me, perfs, withProfile = false) ++ Json
          .obj(
            "prefs" -> lila.pref.JsonView.write(ctx.pref, lichobileCompat = HTTPRequest.isLichobile(req)),
            "nowPlaying" -> JsArray(povs.take(50).map(env.api.lobbyApi.nowPlaying)),
            "nbChallenges" -> nbChallenges,
            "online" -> true
          )
          .add("kid" -> ctx.kid)
          .add("troll" -> me.marks.troll)
          .add("playban" -> playban)
          .add("announce" -> AnnounceApi.get.map(_.json))
      .headerCacheSeconds(15)
  }

  def nowPlaying = Auth { _ ?=> _ ?=>
    negotiateJson(doNowPlaying)
  }

  val apiMe = Scoped() { ctx ?=> me ?=>
    def limited = rateLimited:
      "Please don't poll this endpoint. Stream https://lichess.org/api#tag/Board/operation/apiStreamEvent instead."
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
        _.map(env.api.lobbyApi.nowPlaying)
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

  def passwd = Auth { _ ?=> me ?=>
    env.security.forms.passwdChange.flatMap: form =>
      Ok.page(pages.password(form))
  }

  def passwdApply = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.passwdChange.flatMap: form =>
        FormFuResult(form)(err => renderPage(pages.password(err))): data =>
          val newPass = ClearPassword(data.newPasswd1)
          for
            _ <- env.security.authenticator.setPassword(me, newPass)
            pwned <- env.security.pwned.isPwned(newPass)
            res <- refreshSessionId(Redirect(routes.Account.passwd).flashSuccess, pwned)
          yield res
  }

  private def refreshSessionId(result: Result, pwned: IsPwned)(using ctx: Context, me: Me): Fu[Result] = for
    _ <- env.security.store.closeAllSessionsOf(me)
    _ <- env.push.webSubscriptionApi.unsubscribeByUser(me)
    _ <- env.push.unregisterDevices(me)
    sessionId <- env.security.api.saveAuthentication(me, ctx.mobileApiVersion, pwned)
  yield result.withCookies(env.security.lilaCookie.session(env.security.api.sessionIdKey, sessionId.value))

  private def emailForm(using me: Me) =
    env.user.repo.email(me).flatMap(env.security.forms.changeEmail)

  def email = Auth { _ ?=> me ?=>
    if getBool("check")
    then Ok.page(renderCheckYourEmail).map(_.hasPersonalData)
    else
      for
        f <- emailForm
        res <- Ok.page(pages.email(f))
      yield res.hasPersonalData
  }

  def apiEmail = Scoped(_.Email.Read) { _ ?=> me ?=>
    Found(env.user.repo.email(me)): email =>
      JsonOk(Json.obj("email" -> email.value))
  }

  def renderCheckYourEmail(using Context) =
    views.auth.checkYourEmail(lila.security.EmailConfirm.cookie.get(ctx.req).map(_.email))

  def emailApply = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.preloadEmailDns() >> emailForm.flatMap: form =>
        FormFuResult(form)(err => renderPage(pages.email(err))): data =>
          val newUserEmail = lila.security.EmailConfirm.UserEmail(me.username, data.email)
          auth.EmailConfirmRateLimit(newUserEmail, ctx.req, rateLimited):
            env.security.emailChange
              .send(me, newUserEmail.email)
              .inject(Redirect(routes.Account.email).flashSuccess:
                lila.core.i18n.I18nKey.site.checkYourEmail.txt())
  }

  def emailConfirm(token: String) = Open:
    Found(env.security.emailChange.confirm(token)): (user, prevEmail) =>
      for
        _ <- prevEmail.exists(_.isNoReply).so(env.clas.api.student.release(user))
        res <- auth.authenticateUser(
          user,
          pwned = IsPwned.No,
          remember = true,
          result =
            if prevEmail.exists(_.isNoReply)
            then Some(_ => Redirect(routes.User.show(user.username)).flashSuccess)
            else Some(_ => Redirect(routes.Account.email).flashSuccess)
        )
      yield res

  def emailConfirmHelp = OpenBody:
    import lila.security.EmailConfirm.Help.*
    ctx.me match
      case Some(me) =>
        Redirect(routes.User.show(me.username))
      case None if get("username").isEmpty =>
        Ok.page(views.account.security.emailConfirmHelp(helpForm, none))
      case None =>
        bindForm(helpForm)(
          err => BadRequest.page(views.account.security.emailConfirmHelp(err, none)),
          username =>
            getStatus(env.user.api, env.user.repo, username).flatMap: status =>
              Ok.page(views.account.security.emailConfirmHelp(helpForm.fill(username), status.some))
        )

  def twoFactor = Auth { _ ?=> me ?=>
    if me.totpSecret.isDefined
    then
      env.security.forms.disableTwoFactor.flatMap: f =>
        Ok.page(views.account.twoFactor.disable(f))
    else
      env.security.forms.setupTwoFactor.flatMap: f =>
        Ok.page(views.account.twoFactor.setup(f))
  }

  def setupTwoFactor = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.setupTwoFactor.flatMap: form =>
        FormFuResult(form)(err => renderPage(views.account.twoFactor.setup(err))): data =>
          for
            _ <- env.user.repo.setupTwoFactor(me, lila.user.TotpSecret.decode(data.secret))
            res <- refreshSessionId(Redirect(routes.Account.twoFactor).flashSuccess, pwned = IsPwned.No)
          yield res
  }

  def disableTwoFactor = AuthBody { ctx ?=> me ?=>
    auth.HasherRateLimit:
      env.security.forms.disableTwoFactor.flatMap: form =>
        FormFuResult(form)(err => renderPage(views.account.twoFactor.disable(err))): _ =>
          env.user.repo.disableTwoFactor(me).inject(Redirect(routes.Account.twoFactor).flashSuccess)
  }

  def close = Auth { _ ?=> me ?=>
    for
      managed <- env.clas.api.student.isManaged(me)
      form <- env.security.forms.closeAccount
      res <- Ok.page(pages.close(form, managed))
    yield res
  }

  def closeConfirm = AuthBody { ctx ?=> me ?=>
    NotManaged:
      auth.HasherRateLimit:
        env.security.forms.closeAccount.flatMap: form =>
          FormFuResult(form)(err => renderPage(pages.close(err, managed = false))): forever =>
            for _ <- env.api.accountTermination.disable(me.value, forever = forever)
            yield Redirect(routes.Lobby.home).withCookies(env.security.lilaCookie.newSession)
  }

  def delete = Auth { _ ?=> me ?=>
    for
      managed <- env.clas.api.student.isManaged(me)
      form <- env.security.forms.deleteAccount
      res <- Ok.page(pages.delete(form, managed))
    yield res
  }

  def deleteConfirm = AuthBody { ctx ?=> me ?=>
    NotManaged:
      auth.HasherRateLimit:
        env.security.forms.deleteAccount.flatMap: form =>
          FormFuResult(form)(err => renderPage(pages.delete(err, managed = false))): _ =>
            for _ <- env.api.accountTermination.scheduleDelete(me.value)
            yield Redirect(routes.Account.deleteDone).withCookies(env.security.lilaCookie.newSession)
  }

  def deleteDone = Open { ctx ?=>
    if ctx.isAuth then Redirect(routes.Lobby.home)
    else FoundPage(env.cms.renderKey("delete-done"))(views.cms.lone)
  }

  def kid = Auth { _ ?=> me ?=>
    for
      managed <- env.clas.api.student.isManaged(me)
      form <- env.security.forms.toggleKid
      content <- env.cms.renderKey("kid-mode")
      page <- Ok.page(pages.kid(me, form, managed, content.map(_.html)))
    yield page
  }
  def apiKid = Scoped(_.Preference.Read) { _ ?=> me ?=>
    JsonOk(Json.obj("kid" -> me.kid.yes))
  }

  def kidPost = AuthBody { ctx ?=> me ?=>
    NotManaged:
      env.security.forms.toggleKid.flatMap: form =>
        bindForm(form)(
          err =>
            negotiate(
              for
                content <- env.cms.renderKey("kid-mode")
                page <- BadRequest.page(pages.kid(me, err, managed = false, content.map(_.html)))
              yield page,
              BadRequest(errorsAsJson(err))
            ),
          _ =>
            for
              _ <- env.user.repo.setKid(me, getBoolAs[KidMode]("v"))
              res <- negotiate(
                Redirect(routes.Account.kid).flashSuccess,
                jsonOkResult
              )
            yield res
        )
  }

  def apiKidPost = Scoped(_.Preference.Write) { ctx ?=> me ?=>
    getBoolOptAs[KidMode]("v") match
      case None => BadRequest(jsonError("Missing v parameter"))
      case Some(v) => env.user.repo.setKid(me, v).inject(jsonOkResult)
  }

  def security = Auth { _ ?=> me ?=>
    for
      _ <- env.security.api.dedup(me, req)
      sessions <- env.security.api.locatedOpenSessions(me, 50)
      clients <- env.oAuth.tokenApi.listClients(50)
      personalAccessTokens <- env.oAuth.tokenApi.countPersonal
      currentSessionId = env.security.api.reqSessionId(req)
      page <- Ok.async:
        views.account.security(me, sessions, currentSessionId, clients, personalAccessTokens)
    yield page.hasPersonalData
  }

  def signout(sessionId: String) = Auth { _ ?=> me ?=>
    if sessionId == "all"
    then refreshSessionId(Redirect(routes.Account.security).flashSuccess, pwned = IsPwned.No)
    else
      for
        _ <- env.security.store.closeUserAndSessionId(me, SessionId(sessionId))
        _ <- env.push.webSubscriptionApi.unsubscribeBySession(SessionId(sessionId))
      yield NoContent
  }

  private def renderReopen(form: Option[Form[Reopen]], msg: Option[String])(using Context) =
    env.security.forms.reopen.map: baseForm =>
      pages.reopen.form(form.foldLeft(baseForm)(_.withForm(_)), msg)

  def reopen = Open:
    auth.RedirectToProfileIfLoggedIn:
      Ok.async(renderReopen(none, none))

  def reopenApply = OpenBody:
    env.security.hcaptcha.verify().flatMap { captcha =>
      if captcha.ok then
        env.security.forms.reopen.flatMap:
          _.form
            .bindFromRequest()
            .fold(
              err => BadRequest.async(renderReopen(err.some, none)),
              data =>
                env.security.reopen
                  .prepare(data.username, data.email, env.mod.logApi.closedByMod)
                  .flatMap {
                    case Left((code, msg)) =>
                      lila.mon.user.auth.reopenRequest(code).increment()
                      BadRequest.async(renderReopen(none, msg.some))
                    case Right(user) =>
                      env.security.loginToken.rateLimit[Result](user, data.email, ctx.req, rateLimited):
                        lila.mon.user.auth.reopenRequest("success").increment()
                        env.security.reopen
                          .send(user, data.email)
                          .inject(Redirect(routes.Account.reopenSent))
                  }
            )
      else BadRequest.async(renderReopen(none, none))
    }

  def reopenSent = Open:
    Ok.page(pages.reopen.sent)

  def reopenLogin(token: String) = Open:
    env.security.reopen
      .confirm(token)
      .flatMap:
        case None =>
          lila.mon.user.auth.reopenConfirm("token_fail").increment()
          notFound
        case Some(user) =>
          for
            _ <- env.report.api.reopenReports(lila.report.Suspect(user))
            result <- auth.authenticateUser(user, remember = true, pwned = IsPwned.No)
            _ = lila.mon.user.auth.reopenConfirm("success").increment()
          yield result

  def data = Auth { _ ?=> me ?=>
    meOrFetch(getUserStr("user"))
      .map:
        _.filter(u => ctx.is(u) || isGrantedOpt(_.Impersonate))
      .orNotFound: user =>
        if getBool("text") then
          apiC.GlobalConcurrencyLimitUser(me)(env.api.personalDataExport(user)): source =>
            Ok.chunked(source.map(_ + "\n")).asAttachmentStream(s"lichess_${user.username}.txt")
        else Ok.page(pages.data(user))
  }
