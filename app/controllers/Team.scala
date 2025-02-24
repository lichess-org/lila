package controllers
package team

import play.api.data.Form
import play.api.libs.json.*
import play.api.mvc.*

import lila.app.{ *, given }
import lila.common.HTTPRequest
import lila.common.Json.given
import lila.core.LightUser
import lila.team.{ Requesting, Team as TeamModel, TeamMember, TeamSecurity }

final class Team(env: Env) extends LilaController(env):

  import env.team.{ forms, api, paginator }

  def all(page: Int) = Open:
    Reasonable(page):
      Ok.async:
        paginator
          .popularTeams(page)
          .map:
            views.team.list.all(_)

  def home(page: Int) = Open:
    ctx.me
      .so(api.hasTeams(_))
      .map:
        if _ then Redirect(routes.Team.mine)
        else Redirect(routes.Team.all(page))

  def show(id: TeamId, page: Int, mod: Boolean) = Open:
    Reasonable(page):
      Found(api.team(id)) { renderTeam(_, page, mod && canEnterModView) }

  def members(id: TeamId, page: Int) = Open:
    Reasonable(page, Max(50)):
      Found(api.teamEnabled(id)): team =>
        val canSee =
          fuccess(team.publicMembers || isGrantedOpt(_.ManageTeam)) >>| ctx.userId.so:
            api.belongsTo(team.id, _)
        canSee.flatMap:
          if _ then
            Ok.async:
              paginator
                .teamMembersWithDate(team, page)
                .map:
                  views.team.membersPage(team, _)
          else authorizationFailed

  def search(text: String, page: Int) = OpenBody:
    Reasonable(page):
      Ok.async:
        if text.trim.isEmpty
        then paginator.popularTeams(page).map { views.team.list.all(_) }
        else
          for
            ids   <- env.teamSearch(text, page)
            teams <- ids.mapFutureList(env.team.teamRepo.byOrderedIds)
            forMe <- teams.mapFutureList(env.team.memberRepo.addMyLeadership)
          yield views.team.list.search(text, forMe)

  private def renderTeam(team: TeamModel, page: Int, asMod: Boolean)(using ctx: Context) = for
    team    <- api.withLeaders(team)
    info    <- env.teamInfo(team, ctx.me, withForum = canHaveForum(team.team, asMod))
    members <- paginator.teamMembers(team.team, page)
    log     <- (asMod && isGrantedOpt(_.ManageTeam)).so(env.mod.logApi.teamLog(team.id))
    hasChat = canHaveChat(info, asMod)
    chat <- hasChat.soFu(env.chat.api.userChat.cached.findMine(team.id.into(ChatId)))
    _ <- env.user.lightUserApi.preloadMany:
      info.publicLeaders.map(_.user) ::: info.userIds
    version <- hasChat.soFu(env.team.version(team.id))
    page    <- renderPage(views.team.show(team, members, info, chat, version, asMod, log))
  yield Ok(page).withCanonical(routes.Team.show(team.id))

  private def canEnterModView(using Context) =
    isGrantedOpt(_.Shusher) || isGrantedOpt(_.ManageTeam)

  private def canHaveChat(info: lila.app.mashup.TeamInfo, requestModView: Boolean)(using
      ctx: Context
  ): Boolean =
    import info.*
    team.enabled && !team.isChatFor(_.None) && ctx.kid.no && HTTPRequest.isHuman(ctx.req) && {
      (team.isChatFor(_.Leaders) && info.ledByMe) ||
      (team.isChatFor(_.Members) && info.mine) ||
      (canEnterModView && requestModView)
    }

  private def canHaveForum(team: TeamModel, asMod: Boolean)(member: Option[TeamMember])(using
      ctx: Context
  ): Boolean =
    team.enabled && !team.isForumFor(_.None) && ctx.kid.no && {
      team.isForumFor(_.Everyone) ||
      (team.isForumFor(_.Leaders) && member.exists(_.perms.nonEmpty)) ||
      (team.isForumFor(_.Members) && member.isDefined) ||
      (isGrantedOpt(_.ModerateForum) && asMod)
    }

  def tournaments(teamId: TeamId) = Open:
    FoundPage(api.teamEnabled(teamId)): team =>
      env.teamInfo
        .tournaments(team, 30, 30)
        .map:
          views.team.tournaments.page(team, _)

  private def renderEdit(team: TeamModel, form: Form[?])(using me: Me, ctx: Context) = for
    member <- env.team.memberRepo.get(team.id, me)
    _      <- env.msg.twoFactorReminder(me)
  yield views.team.form.edit(team, form, member)

  def edit(id: TeamId) = Auth { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id, _.Settings): team =>
      Ok.async(renderEdit(team, forms.edit(team)))
  }

  def update(id: TeamId) = AuthBody { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id, _.Settings): team =>
      bindForm(forms.edit(team))(
        err => BadRequest.async(renderEdit(team, err)),
        data => api.update(team, data).inject(Redirect(routes.Team.show(team.id)).flashSuccess)
      )
  }

  def kickForm(id: TeamId) = Auth { ctx ?=> _ ?=>
    WithOwnedTeamEnabled(id, _.Kick): team =>
      api.blocklist
        .get(team)
        .flatMap: blocklist =>
          Ok.page(views.team.admin.kick(team, forms.members, forms.blocklist.fill(blocklist)))
  }

  def kick(id: TeamId) = AuthBody { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id, _.Kick): team =>
      forms.members
        .bindFromRequest()
        .value
        .so { api.kickMembers(team, _) }
        .inject(Redirect(routes.Team.show(team.id)).flashSuccess)
  }

  def blocklist(id: TeamId) = AuthBody { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id, _.Kick): team =>
      forms.blocklist
        .bindFromRequest()
        .value
        .so { api.blocklist.set(team, _) }
        .inject(Redirect(routes.Team.show(team.id)).flashSuccess)
  }

  def leaders(id: TeamId) = Auth { ctx ?=> _ ?=>
    WithOwnedTeamEnabled(id, _.Admin): team =>
      api
        .withLeaders(team)
        .flatMap: team =>
          Ok.page(leadersPage(team, None, None))
  }

  def permissions(id: TeamId) = AuthBody { ctx ?=> me ?=>
    WithOwnedTeamEnabledWithModInfo(id, _.Admin): (team, asMod) =>
      api
        .withLeaders(team)
        .flatMap: team =>
          bindForm(env.team.security.form.permissions(team))(
            err => BadRequest.page(leadersPage(team, None, err.some)),
            data =>
              env.team.security
                .setPermissions(team, data)
                .flatMap:
                  _.filter(_.user.isnt(me))
                    .sequentially: change =>
                      env.msg.api.systemPost(
                        change.user,
                        lila.msg.MsgPreset.newPermissions(
                          if asMod then LightUser.fallback(UserName.lichess) else me.light,
                          team.team.light,
                          change.perms.map(_.name),
                          env.net.baseUrl
                        )
                      )
                    .inject:
                      Redirect(routes.Team.leaders(team.id)).flashSuccess
          )
  }

  def addLeader(id: TeamId) = AuthBody { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id, _.Admin): team =>
      api
        .withLeaders(team)
        .flatMap: team =>
          bindForm(env.team.security.form.addLeader(team))(
            err => BadRequest.page(leadersPage(team, err.some, None)),
            name =>
              env.team.security
                .addLeader(team, name)
                .inject(Redirect(routes.Team.leaders(team.id)).flashSuccess)
          )
  }

  private def leadersPage(
      team: TeamModel.WithLeaders,
      addLeader: Option[Form[UserStr]] = None,
      permissions: Option[Form[List[TeamSecurity.LeaderData]]] = None
  )(using Context, Me) = views.team.admin.leaders(
    team,
    addLeader | env.team.security.form.addLeader(team),
    permissions | env.team.security.form.permissions(team)
  )

  def close(id: TeamId) = SecureBody(_.ManageTeam) { ctx ?=> me ?=>
    Found(api.team(id)): team =>
      bindForm(forms.explain)(
        _ => funit,
        explain =>
          api.delete(team, me.value, explain) >>
            env.mod.logApi.deleteTeam(team.id, explain)
      )
        .inject(Redirect(routes.Team.all(1)).flashSuccess)
  }

  def disable(id: TeamId) = AuthBody { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id, _.Admin): team =>
      val redirect = Redirect(routes.Team.show(id))
      bindForm(forms.explain)(
        _ => redirect.flashFailure,
        explain =>
          (api.toggleEnabled(team, explain) >>
            env.mod.logApi.toggleTeam(team.id, team.enabled, explain)).inject(redirect.flashSuccess)
      )
  }

  private def LimitPerWeek[A <: Result](a: => Fu[A])(using ctx: Context, me: Me): Fu[Result] =
    api.countCreatedRecently(me).flatMap { count =>
      val allow =
        isGrantedOpt(_.ManageTeam) ||
          (isGrantedOpt(_.Verified) && count < 100) ||
          (isGrantedOpt(_.Teacher) && count < 10) ||
          count < 3
      if allow then a
      else Forbidden.page(views.site.message.teamCreateLimit)
    }

  def form = Auth { ctx ?=> me ?=>
    LimitPerWeek:
      Ok.page(views.team.form.create(forms.create, anyCaptcha))
  }

  private val OneAtATime = lila.web.FutureConcurrencyLimit[UserId](
    key = "team.concurrency.user",
    ttl = 10.minutes,
    maxConcurrency = 1
  )
  def create = AuthBody { ctx ?=> me ?=>
    OneAtATime(me, rateLimited):
      LimitPerWeek:
        JoinLimit(tooManyTeamsHtml):
          bindForm(forms.create)(
            err => BadRequest.page(views.team.form.create(err, anyCaptcha)),
            data =>
              api.create(data, me).map { team =>
                Redirect(routes.Team.show(team.id))
              }
          )
  }

  def mine = Auth { ctx ?=> me ?=>
    Ok.async:
      api.mine.map:
        views.team.list.mine(_)
  }

  private def tooManyTeamsHtml(using Context, Me): Fu[Result] =
    BadRequest.async:
      api.mine.map(views.team.list.mine)
  private val tooManyTeamsJson = BadRequest(jsonError("You have joined too many teams"))

  def leader = Auth { ctx ?=> me ?=>
    Ok.async:
      api.teamsLedBy(me).map(views.team.list.ledByMe)
  }

  private def JoinLimit(tooMany: => Fu[Result])(f: => Fu[Result])(using Me) =
    api.hasJoinedTooManyTeams
      .flatMap:
        if _ then tooMany else f

  def join(id: TeamId) = AuthOrScopedBody(_.Team.Write) { ctx ?=> me ?=>
    Found(api.teamEnabled(id)): team =>
      OneAtATime(me, rateLimited):
        JoinLimit(negotiate(tooManyTeamsHtml, tooManyTeamsJson)):
          negotiate(
            html = webJoin(team, request = none, password = none),
            json = bindForm(forms.apiRequest(team))(
              jsonFormError,
              setup =>
                api
                  .join(team, setup.message, setup.password)
                  .flatMap:
                    case Requesting.Joined       => jsonOkResult
                    case Requesting.NeedRequest  => BadRequest(jsonError("This team requires confirmation."))
                    case Requesting.NeedPassword => BadRequest(jsonError("This team requires a password."))
                    case Requesting.Blocklist    => BadRequest(jsonError("You cannot join this team."))
            )
          )
  }

  def subscribe(teamId: TeamId) =
    AuthOrScopedBody(_.Team.Write) { _ ?=> me ?=>
      bindForm(env.team.forms.subscribe)(_ => funit, v => api.subscribe(teamId, me, ~v))
        .inject(jsonOkResult)
    }

  def requests = Auth { _ ?=> me ?=>
    import lila.memo.CacheApi.*
    env.team.cached.nbRequests.invalidate(me)
    Ok.async:
      api.requestsWithUsers(me).map(views.team.request.all)
  }

  def requestForm(id: TeamId) = Auth { _ ?=> me ?=>
    FoundPage(api.requestable(id)): team =>
      views.team.request.requestForm(team, forms.request(team))
  }

  def requestCreate(id: TeamId) = AuthBody { ctx ?=> me ?=>
    Found(api.requestable(id)): team =>
      OneAtATime(me, rateLimited):
        JoinLimit(tooManyTeamsHtml):
          bindForm(forms.request(team))(
            err => BadRequest.page(views.team.request.requestForm(team, err)),
            setup =>
              if team.open then webJoin(team, request = none, password = setup.password)
              else
                setup.message.so: msg =>
                  api.createRequest(team, msg).inject(Redirect(routes.Team.show(team.id)).flashSuccess)
          )
  }

  private def webJoin(team: TeamModel, request: Option[String], password: Option[String])(using Me) =
    api
      .join(team, request = request, password = password)
      .flatMap:
        case Requesting.Joined => Redirect(routes.Team.show(team.id)).flashSuccess
        case Requesting.NeedRequest | Requesting.NeedPassword =>
          Redirect(routes.Team.requestForm(team.id)).flashSuccess
        case Requesting.Blocklist =>
          Redirect(routes.Team.show(team.id)).flashFailure("You cannot join this team.")

  def requestProcess(requestId: String) = AuthBody { ctx ?=> me ?=>
    Found(for
      requestOption <- api.request(requestId)
      teamOption    <- requestOption.so(req => env.team.teamRepo.byId(req.team))
      isGranted <- teamOption.so: team =>
        api.isGranted(team.id, me, _.Request)
    yield (teamOption.ifTrue(isGranted), requestOption).tupled): (team, request) =>
      bindForm(forms.processRequest)(
        _ => Redirect(routes.Team.show(team.id)),
        (decision, url) => api.processRequest(team, request, decision).inject(Redirect(url))
      )
  }

  def declinedRequests(id: TeamId, page: Int) = AuthBody { ctx ?=> _ ?=>
    WithOwnedTeamEnabled(id, _.Request): team =>
      bindForm(forms.searchDeclinedForm)(
        _ => BadRequest(""),
        userQuery =>
          Ok.async:
            paginator.declinedRequests(team, page, userQuery).map {
              views.team.request.declined(team, _, userQuery)
            }
      )
  }

  def quit(id: TeamId) = AuthOrScoped(_.Team.Write) { ctx ?=> me ?=>
    Found(api.team(id)): team =>
      api
        .withLeaders(team)
        .flatMap: t =>
          val admins = t.leaders.filter(_.hasPerm(_.Admin))
          if admins.nonEmpty && admins.forall(_.is(me))
          then
            val msg = lila.core.i18n.I18nKey.team.onlyLeaderLeavesTeam.txt()
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
          _     <- env.user.lightUserApi.preloadMany(teams.map(_.createdBy))
        yield JsonOk:
          JsArray(teams.map: team =>
            Json.obj(
              "id"      -> team.id,
              "name"    -> team.name,
              "owner"   -> env.user.lightUserApi.syncFallback(team.createdBy).name,
              "members" -> team.nbMembers
            ))

  def pmAll(id: TeamId) = Auth { ctx ?=> _ ?=>
    WithOwnedTeamEnabled(id, _.PmAll): team =>
      renderPmAll(team, forms.pmAll)
  }

  private def renderPmAll(team: TeamModel, form: Form[?])(using Context) = for
    tours   <- env.tournament.api.visibleByTeam(team.id, 0, 20).dmap(_.next)
    unsubs  <- env.team.cached.unsubs.get(team.id)
    limiter <- env.teamInfo.pmAll.status(team.id)
    page    <- renderPage(views.team.admin.pmAll(team, form, tours, unsubs, limiter))
  yield Ok(page)

  def pmAllSubmit(id: TeamId) = AuthOrScopedBody(_.Team.Lead) { ctx ?=> me ?=>
    WithOwnedTeamEnabled(id, _.PmAll): team =>
      import lila.memo.RateLimit.Result
      bindForm(forms.pmAll)(
        Left(_),
        msg =>
          if env.teamInfo.pmAll.dedup(team.id, msg) then
            Right:
              env.teamInfo.pmAll.limiter[Result](
                team.id,
                if me.isVerifiedOrAdmin then 1 else mashup.TeamInfo.pmAllCost
              ) {
                val url = s"${env.net.baseUrl}${routes.Team.show(team.id)}"
                val full = s"""$msg
  ---
  You received this because you are subscribed to messages of the team $url."""
                env.msg.api
                  .multiPost(
                    env.team.memberStream.subscribedIds(team, MaxPerSecond(50)),
                    full
                  )
                  .addEffect(lila.mon.msg.teamBulk.record(_))
                // we don't wait for the stream to complete, it would make lichess time out
                fuccess(Result.Through)
              }(Result.Limited)
          else Left(forms.pmAll.withError("duplicate", "You already sent this message recently"))
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

  private def WithOwnedTeam(teamId: TeamId, perm: TeamSecurity.Permission.Selector)(
      f: (TeamModel, AsMod) => Fu[Result]
  )(using Context): Fu[Result] =
    Found(api.team(teamId)): team =>
      ctx.userId
        .so(api.hasPerm(team.id, _, perm))
        .flatMap: isGrantedLeader =>
          val asMod = !isGrantedLeader && isGrantedOpt(_.ManageTeam)
          if isGrantedLeader || asMod then f(team, asMod)
          else Redirect(routes.Team.show(team.id))

  private def WithOwnedTeamEnabledWithModInfo(
      teamId: TeamId,
      perm: TeamSecurity.Permission.Selector
  )(f: (TeamModel, AsMod) => Fu[Result])(using Context): Fu[Result] =
    WithOwnedTeam(teamId, perm): (team, asMod) =>
      if team.enabled || isGrantedOpt(_.ManageTeam) then f(team, asMod)
      else notFound

  private def WithOwnedTeamEnabled(
      teamId: TeamId,
      perm: TeamSecurity.Permission.Selector
  )(f: TeamModel => Fu[Result])(using Context): Fu[Result] =
    WithOwnedTeamEnabledWithModInfo(teamId, perm): (team, _) =>
      if team.enabled || isGrantedOpt(_.ManageTeam) then f(team)
      else notFound

  opaque type AsMod = Boolean
  object AsMod extends TotalWrapper[AsMod, Boolean]
