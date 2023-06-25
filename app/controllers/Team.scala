package controllers

import play.api.data.{ Forms, Form }
import play.api.data.Forms.*
import play.api.libs.json.*
import play.api.mvc.*
import views.*

import lila.app.{ given, * }
import lila.common.{ config, HTTPRequest, IpAddress }
import lila.memo.RateLimit
import lila.team.{ Requesting, Team as TeamModel }
import lila.user.{ User as UserModel }
import Api.ApiResult

final class Team(
    env: Env,
    apiC: => Api
) extends LilaController(env):

  private def forms     = env.team.forms
  private def api       = env.team.api
  private def paginator = env.team.paginator

  def all(page: Int) = Open:
    Reasonable(page):
      Ok.pageAsync:
        paginator popularTeams page map {
          html.team.list.all(_)
        }

  def home(page: Int) = Open:
    ctx.me.so(api.hasTeams(_)) map {
      if _ then Redirect(routes.Team.mine)
      else Redirect(routes.Team.all(page))
    }

  def show(id: TeamId, page: Int, mod: Boolean) = Open:
    Reasonable(page):
      Found(api team id) { renderTeam(_, page, mod) }

  def members(id: TeamId, page: Int) = Open:
    Reasonable(page, config.Max(50)):
      Found(api teamEnabled id): team =>
        val canSee =
          fuccess(team.publicMembers || isGrantedOpt(_.ManageTeam)) >>| ctx.userId.so {
            api.belongsTo(team.id, _)
          }
        canSee flatMap {
          if _ then
            Ok.pageAsync:
              paginator.teamMembersWithDate(team, page) map {
                html.team.members(team, _)
              }
          else authorizationFailed
        }

  def search(text: String, page: Int) = OpenBody:
    Reasonable(page):
      Ok.pageAsync:
        if text.trim.isEmpty
        then paginator popularTeams page map { html.team.list.all(_) }
        else env.teamSearch(text, page) map { html.team.list.search(text, _) }

  private def renderTeam(team: TeamModel, page: Int, requestModView: Boolean)(using ctx: Context) = for
    info    <- env.teamInfo(team, ctx.me, withForum = canHaveForum(team, requestModView))
    members <- paginator.teamMembers(team, page)
    log     <- (requestModView && isGrantedOpt(_.ManageTeam)).so(env.mod.logApi.teamLog(team.id))
    hasChat = canHaveChat(team, info, requestModView)
    chat <-
      hasChat so env.chat.api.userChat.cached
        .findMine(ChatId(team.id))
        .map(some)
    _ <- env.user.lightUserApi preloadMany {
      team.leaders.toList ::: info.userIds ::: chat.so(_.chat.userIds)
    }
    version <- hasChat so env.team.version(team.id).dmap(some)
    page    <- renderPage(html.team.show(team, members, info, chat, version, requestModView, log))
  yield Ok(page).withCanonical(routes.Team.show(team.id))

  private def canHaveChat(team: TeamModel, info: lila.app.mashup.TeamInfo, requestModView: Boolean)(using
      ctx: Context
  ): Boolean =
    team.enabled && !team.isChatFor(_.NONE) && ctx.noKid && HTTPRequest.isHuman(ctx.req) && {
      (team.isChatFor(_.LEADERS) && ctx.userId.exists(team.leaders)) ||
      (team.isChatFor(_.MEMBERS) && info.mine) ||
      (isGrantedOpt(_.Shusher) && requestModView)
    }

  private def canHaveForum(team: TeamModel, requestModView: Boolean)(isMember: Boolean)(using
      ctx: Context
  ): Boolean =
    team.enabled && !team.isForumFor(_.NONE) && ctx.noKid && {
      team.isForumFor(_.EVERYONE) ||
      (team.isForumFor(_.LEADERS) && ctx.userId.exists(team.leaders)) ||
      (team.isForumFor(_.MEMBERS) && isMember) ||
      (isGrantedOpt(_.ModerateForum) && requestModView)
    }

  def users(teamId: TeamId) = AnonOrScoped(_.Team.Read): ctx ?=>
    Found(api teamEnabled teamId): team =>
      val canView: Fu[Boolean] =
        if team.publicMembers then fuccess(true)
        else ctx.me.so(api.belongsTo(team.id, _))
      canView.map:
        if _ then
          apiC.jsonDownload(
            env.team
              .memberStream(team, config.MaxPerSecond(20))
              .map: (user, joinedAt) =>
                env.api.userApi.one(user, joinedAt.some)
          )
        else Unauthorized

  def tournaments(teamId: TeamId) = Open:
    FoundPage(api teamEnabled teamId): team =>
      env.teamInfo.tournaments(team, 30, 30) map {
        html.team.tournaments.page(team, _)
      }

  def edit(id: TeamId) = Auth { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id): team =>
      env.msg.twoFactorReminder(me) >>
        Ok.page(html.team.form.edit(team, forms edit team))
  }

  def update(id: TeamId) = AuthBody { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id): team =>
      forms
        .edit(team)
        .bindFromRequest()
        .fold(
          err => BadRequest.page(html.team.form.edit(team, err)),
          data => api.update(team, data) inject Redirect(routes.Team.show(team.id)).flashSuccess
        )
  }

  def kickForm(id: TeamId) = Auth { ctx ?=> _ ?=>
    WithOwnedTeamEnabled(id): team =>
      Ok.page(html.team.admin.kick(team, forms.members))
  }

  def kick(id: TeamId) = AuthBody { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id): team =>
      forms.members.bindFromRequest().value so { api.kickMembers(team, _).parallel } inject Redirect(
        routes.Team.show(team.id)
      ).flashSuccess
  }

  private val ApiKickRateLimitPerIP = lila.memo.RateLimit.composite[IpAddress](
    key = "team.kick.api.ip",
    enforce = env.net.rateLimit.value
  )(
    ("fast", 10, 2.minutes),
    ("slow", 50, 1.day)
  )
  private val kickLimitReportOnce = lila.memo.OnceEvery[UserId](10.minutes)

  def kickUser(teamId: TeamId, username: UserStr) = Scoped(_.Team.Lead) { ctx ?=> me ?=>
    WithOwnedTeamEnabledApi(teamId): team =>
      def limited =
        if kickLimitReportOnce(username.id) then
          lila
            .log("security")
            .warn(s"API team.kick limited team:${teamId} user:${me.username} ip:${req.ipAddress}")
        fuccess(ApiResult.Limited)
      ApiKickRateLimitPerIP(req.ipAddress, limited, cost = if me.isVerified || me.isApiHog then 0 else 1):
        api.kick(team, username.id) inject ApiResult.Done
  }

  def leadersForm(id: TeamId) = Auth { ctx ?=> _ ?=>
    WithOwnedTeamEnabled(id): team =>
      Ok.page(html.team.admin.leaders(team, forms leaders team))
  }

  def leaders(id: TeamId) = AuthBody { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id): team =>
      forms.leaders(team).bindFromRequest().value so {
        api.setLeaders(team, _, me, isGranted(_.ManageTeam))
      } inject Redirect(
        routes.Team.show(team.id)
      ).flashSuccess
  }

  def close(id: TeamId) = SecureBody(_.ManageTeam) { ctx ?=> me ?=>
    Found(api team id): team =>
      forms.explain
        .bindFromRequest()
        .fold(
          _ => funit,
          explain =>
            api.delete(team, me.value, explain) >>
              env.mod.logApi.deleteTeam(team.id, explain)
        ) inject Redirect(routes.Team all 1).flashSuccess
  }

  def disable(id: TeamId) = AuthBody { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id) { team =>
      val redirect = Redirect(routes.Team show id)
      forms.explain
        .bindFromRequest()
        .fold(
          _ => redirect.flashFailure,
          explain =>
            api.toggleEnabled(team, explain) >>
              env.mod.logApi.toggleTeam(team.id, team.enabled, explain) inject
              redirect.flashSuccess
        )
    }
  }

  def form = Auth { ctx ?=> me ?=>
    LimitPerWeek:
      forms.anyCaptcha flatMap { captcha =>
        Ok.page(html.team.form.create(forms.create, captcha))
      }
  }

  private val OneAtATime = lila.memo.FutureConcurrencyLimit[UserId](
    key = "team.concurrency.user",
    ttl = 10.minutes,
    maxConcurrency = 1
  )
  def create = AuthBody { ctx ?=> me ?=>
    OneAtATime(me, rateLimitedFu):
      LimitPerWeek:
        JoinLimit(tooManyTeamsHtml):
          forms.create
            .bindFromRequest()
            .fold(
              err =>
                BadRequest.pageAsync:
                  forms.anyCaptcha.map(html.team.form.create(err, _))
              ,
              data =>
                api.create(data, me) map { team =>
                  Redirect(routes.Team.show(team.id))
                }
            )
  }

  def mine = Auth { ctx ?=> me ?=>
    Ok.pageAsync:
      api.mine map {
        html.team.list.mine(_)
      }
  }

  private def tooManyTeamsHtml(using Context, Me): Fu[Result] =
    BadRequest.pageAsync:
      api.mine map html.team.list.mine
  private val tooManyTeamsJson = BadRequest(jsonError("You have joined too many teams"))

  def leader = Auth { ctx ?=> me ?=>
    Ok.pageAsync:
      env.team.teamRepo enabledTeamsByLeader me map html.team.list.ledByMe
  }

  private def JoinLimit(tooMany: => Fu[Result])(f: => Fu[Result])(using Me) =
    api.hasJoinedTooManyTeams
      .flatMap:
        if _ then tooMany else f

  def join(id: TeamId) = AuthOrScopedBody(_.Team.Write) { ctx ?=> me ?=>
    Found(api.teamEnabled(id)): team =>
      OneAtATime(me, rateLimitedFu):
        JoinLimit(negotiate(tooManyTeamsHtml, tooManyTeamsJson)):
          negotiate(
            html = webJoin(team, request = none, password = none),
            json = forms
              .apiRequest(team)
              .bindFromRequest()
              .fold(
                jsonFormError,
                setup =>
                  api.join(team, setup.message, setup.password) flatMap {
                    case Requesting.Joined       => jsonOkResult
                    case Requesting.NeedRequest  => BadRequest(jsonError("This team requires confirmation."))
                    case Requesting.NeedPassword => BadRequest(jsonError("This team requires a password."))
                  }
              )
          )
  }

  def subscribe(teamId: TeamId) =
    AuthOrScopedBody(_.Team.Write) { _ ?=> me ?=>
      Form(single("subscribe" -> optional(Forms.boolean)))
        .bindFromRequest()
        .fold(_ => funit, v => api.subscribe(teamId, me, ~v))
        .inject(jsonOkResult)
    }

  def requests = Auth { _ ?=> me ?=>
    import lila.memo.CacheApi.*
    env.team.cached.nbRequests invalidate me
    Ok.pageAsync:
      api requestsWithUsers me map html.team.request.all
  }

  def requestForm(id: TeamId) = Auth { _ ?=> me ?=>
    FoundPage(api.requestable(id)): team =>
      html.team.request.requestForm(team, forms.request(team))
  }

  def requestCreate(id: TeamId) = AuthBody { ctx ?=> me ?=>
    Found(api.requestable(id)): team =>
      OneAtATime(me, rateLimitedFu):
        JoinLimit(tooManyTeamsHtml):
          forms
            .request(team)
            .bindFromRequest()
            .fold(
              err => BadRequest.page(html.team.request.requestForm(team, err)),
              setup =>
                if team.open then webJoin(team, request = none, password = setup.password)
                else
                  setup.message.so: msg =>
                    api.createRequest(team, msg) inject Redirect(routes.Team.show(team.id)).flashSuccess
            )
  }

  private def webJoin(team: TeamModel, request: Option[String], password: Option[String])(using Me) =
    api.join(team, request = request, password = password) flatMap {
      case Requesting.Joined => Redirect(routes.Team.show(team.id)).flashSuccess
      case Requesting.NeedRequest | Requesting.NeedPassword =>
        Redirect(routes.Team.requestForm(team.id)).flashSuccess
    }

  def requestProcess(requestId: String) = AuthBody { ctx ?=> me ?=>
    import cats.syntax.all.*
    Found(for
      requestOption <- api request requestId
      teamOption    <- requestOption.so(req => env.team.teamRepo.byLeader(req.team, me))
    yield (teamOption, requestOption).mapN((_, _))): (team, request) =>
      forms.processRequest
        .bindFromRequest()
        .fold(
          _ => Redirect(routes.Team.show(team.id)),
          (decision, url) => api.processRequest(team, request, decision) inject Redirect(url)
        )
  }

  def declinedRequests(id: TeamId, page: Int) = Auth { ctx ?=> _ ?=>
    WithOwnedTeamEnabled(id): team =>
      Ok.pageAsync:
        paginator.declinedRequests(team, page) map {
          html.team.declinedRequest.all(team, _)
        }
  }

  def quit(id: TeamId) = AuthOrScoped(_.Team.Write) { ctx ?=> me ?=>
    Found(api team id): team =>
      if team isOnlyLeader me then
        val msg = lila.i18n.I18nKeys.team.onlyLeaderLeavesTeam.txt()
        negotiate(
          html = Redirect(routes.Team.edit(team.id)).flashFailure(msg),
          json = JsonBadRequest(msg)
        )
      else
        api.cancelRequestOrQuit(team) >>
          negotiate(
            html = Redirect(routes.Team.mine).flashSuccess,
            json = jsonOkResult
          )
  }

  def autocomplete = Anon:
    get("term").filter(_.nonEmpty) match
      case None => BadRequest("No search term provided")
      case Some(term) =>
        for
          teams <- api.autocomplete(term, 10)
          _     <- env.user.lightUserApi preloadMany teams.map(_.createdBy)
        yield JsonOk:
          JsArray(teams.map: team =>
            Json.obj(
              "id"      -> team.id,
              "name"    -> team.name,
              "owner"   -> env.user.lightUserApi.syncFallback(team.createdBy).name,
              "members" -> team.nbMembers
            ))

  def pmAll(id: TeamId) = Auth { ctx ?=> _ ?=>
    WithOwnedTeamEnabled(id): team =>
      renderPmAll(team, forms.pmAll)
  }

  private def renderPmAll(team: TeamModel, form: Form[?])(using Context) = for
    tours   <- env.tournament.api.visibleByTeam(team.id, 0, 20).dmap(_.next)
    unsubs  <- env.team.cached.unsubs.get(team.id)
    limiter <- env.teamInfo.pmAllStatus(team.id)
    page    <- renderPage(html.team.admin.pmAll(team, form, tours, unsubs, limiter))
  yield Ok(page)

  def pmAllSubmit(id: TeamId) = AuthOrScopedBody(_.Team.Lead) { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id): team =>
      import RateLimit.Result
      forms.pmAll
        .bindFromRequest()
        .fold(
          err => Left(err),
          msg =>
            Right:
              env.teamInfo.pmAllLimiter[Result](
                team.id,
                if me.isVerifiedOrAdmin then 1 else mashup.TeamInfo.pmAllCost
              ) {
                val url = s"${env.net.baseUrl}${routes.Team.show(team.id)}"
                val full = s"""$msg
  ---
  You received this because you are subscribed to messages of the team $url."""
                env.msg.api
                  .multiPost(
                    env.team.memberStream.subscribedIds(team, config.MaxPerSecond(50)),
                    full
                  )
                  .addEffect: nb =>
                    lila.mon.msg.teamBulk(team.id).record(nb).unit
                // we don't wait for the stream to complete, it would make lichess time out
                fuccess(Result.Through)
              }(Result.Limited)
        )
        .fold(
          err => negotiate(renderPmAll(team, err), BadRequest(errorsAsJson(err))),
          _.flatMap: res =>
            negotiate(
              Redirect(routes.Team.show(team.id)).flashing:
                res match
                  case Result.Through => "success" -> ""
                  case Result.Limited => "failure" -> rateLimitedMsg
              ,
              res match
                case Result.Through => jsonOkResult
                case Result.Limited => rateLimitedJson
            )
        )
  }

  // API

  def apiAll(page: Int) = Anon:
    import env.team.jsonView.given
    import lila.common.paginator.PaginatorJson.given
    JsonOk:
      paginator popularTeams page flatMap { pager =>
        env.user.lightUserApi.preloadMany(pager.currentPageResults.flatMap(_.leaders)) inject pager
      }

  def apiShow(id: TeamId) = Open:
    JsonOptionOk:
      api teamEnabled id flatMapz { team =>
        for
          joined    <- ctx.userId.so { api.belongsTo(id, _) }
          requested <- ctx.userId.ifFalse(joined).so { env.team.requestRepo.exists(id, _) }
        yield {
          env.team.jsonView.teamWrites.writes(team) ++ Json
            .obj(
              "joined"    -> joined,
              "requested" -> requested
            )
        }.some
      }

  def apiSearch(text: String, page: Int) = Anon:
    import env.team.jsonView.given
    import lila.common.paginator.PaginatorJson.given
    JsonOk:
      if text.trim.isEmpty
      then paginator popularTeams page
      else env.teamSearch(text, page)

  def apiTeamsOf(username: UserStr) = AnonOrScoped(): ctx ?=>
    import env.team.jsonView.given
    JsonOk:
      api.joinedTeamIdsOfUserAsSeenBy(username) flatMap api.teamsByIds flatMap { teams =>
        env.user.lightUserApi.preloadMany(teams.flatMap(_.leaders)) inject teams
      }

  def apiRequests(teamId: TeamId) = Scoped(_.Team.Read) { ctx ?=> me ?=>
    WithOwnedTeamEnabledApi(teamId): team =>
      import env.team.jsonView.requestWithUserWrites
      val reqs =
        if getBool("declined") then api.declinedRequestsWithUsers(team)
        else api.requestsWithUsers(team)
      reqs map Json.toJson map ApiResult.Data.apply

  }

  def apiRequestProcess(teamId: TeamId, userId: UserStr, decision: String) = Scoped(_.Team.Lead) {
    _ ?=> me ?=>
      WithOwnedTeamEnabledApi(teamId): team =>
        api request lila.team.Request.makeId(team.id, userId.id) flatMap {
          case None      => fuccess(ApiResult.ClientError("No such team join request"))
          case Some(req) => api.processRequest(team, req, decision) inject ApiResult.Done
        }
  }

  private def LimitPerWeek[A <: Result](a: => Fu[A])(using ctx: Context, me: Me): Fu[Result] =
    api.countCreatedRecently(me) flatMap { count =>
      val allow =
        isGrantedOpt(_.ManageTeam) ||
          (isGrantedOpt(_.Verified) && count < 100) ||
          (isGrantedOpt(_.Teacher) && count < 10) ||
          count < 3
      if allow then a
      else Forbidden.page(views.html.site.message.teamCreateLimit)
    }

  private def WithOwnedTeam(teamId: TeamId)(f: TeamModel => Fu[Result])(using Context): Fu[Result] =
    Found(api team teamId): team =>
      if ctx.userId.exists(team.leaders.contains) || isGrantedOpt(_.ManageTeam) then f(team)
      else Redirect(routes.Team.show(team.id))

  private def WithOwnedTeamEnabled(
      teamId: TeamId
  )(f: TeamModel => Fu[Result])(using Context): Fu[Result] =
    WithOwnedTeam(teamId): team =>
      if team.enabled || isGrantedOpt(_.ManageTeam) then f(team)
      else notFound

  private def WithOwnedTeamEnabledApi(teamId: TeamId)(
      f: TeamModel => Fu[ApiResult]
  )(using me: Me): Fu[Result] =
    api teamEnabled teamId flatMap {
      case Some(team) if team leaders me => f(team)
      case Some(_)                       => fuccess(ApiResult.ClientError("Not your team"))
      case None                          => fuccess(ApiResult.NoData)
    } map apiC.toHttp
