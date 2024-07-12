package controllers

import play.api.libs.json.*
import play.api.mvc.*
import scalalib.Json.given

import lila.app.{ *, given }
import lila.core.net.IpAddress
import lila.team.{ Team as TeamModel, TeamSecurity }

import Api.ApiResult

final class TeamApi(env: Env, apiC: => Api) extends LilaController(env):

  private def api       = env.team.api
  private def paginator = env.team.paginator

  def all(page: Int) = Anon:
    import env.team.jsonView.given
    JsonOk:
      for
        pager <- paginator.popularTeamsWithPublicLeaders(page)
        _     <- env.user.lightUserApi.preloadMany(pager.currentPageResults.flatMap(_.publicLeaders))
      yield pager

  def show(id: TeamId) = OpenOrScoped(): ctx ?=>
    JsonOptionOk:
      api
        .teamEnabled(id)
        .flatMapz: team =>
          for
            joined      <- ctx.userId.so { api.belongsTo(id, _) }
            requested   <- ctx.userId.ifFalse(joined).so { env.team.requestRepo.exists(id, _) }
            withLeaders <- env.team.memberRepo.addPublicLeaderIds(team)
            _           <- env.user.lightUserApi.preloadMany(withLeaders.publicLeaders)
          yield some:
            import env.team.jsonView.given
            import lila.common.Json.given
            Json.toJsObject(withLeaders) ++ Json
              .obj(
                "joined"    -> joined,
                "requested" -> requested
              )
              .add("descriptionPrivate" -> team.descPrivate.ifTrue(joined))

  def users(teamId: TeamId) = AnonOrScoped(_.Team.Read): ctx ?=>
    Found(api.teamEnabled(teamId)): team =>
      val canView: Fu[Boolean] =
        if team.publicMembers then fuccess(true)
        else ctx.me.so(api.belongsTo(team.id, _))
      canView.map:
        if _ then
          val full = getBool("full")
          apiC.jsonDownload(
            env.team
              .memberStream(team, full)
              .map: (user, joinedAt) =>
                env.api.userApi.one(user, joinedAt.some)
          )
        else Unauthorized

  def search(text: String, page: Int) = Anon:
    import env.team.jsonView.given
    JsonOk:
      if text.trim.isEmpty
      then paginator.popularTeamsWithPublicLeaders(page)
      else
        for
          ids   <- env.teamSearch(text, page)
          teams <- ids.mapFutureList(env.team.teamRepo.byOrderedIds)
          leads <- teams.mapFutureList(env.team.memberRepo.addPublicLeaderIds)
        yield leads

  def teamsOf(username: UserStr) = AnonOrScoped(): ctx ?=>
    import env.team.jsonView.given
    JsonOk:
      for
        ids   <- api.joinedTeamIdsOfUserAsSeenBy(username)
        teams <- api.teamsByIds(ids)
        teams <- env.team.memberRepo.addPublicLeaderIds(teams)
        _     <- env.user.lightUserApi.preloadMany(teams.flatMap(_.publicLeaders))
      yield teams

  def requests(teamId: TeamId) = Scoped(_.Team.Read) { ctx ?=> me ?=>
    WithOwnedTeamEnabled(teamId, _.Request): team =>
      import env.team.jsonView.requestWithUserWrites
      val reqs =
        if getBool("declined") then api.declinedRequestsWithUsers(team)
        else api.requestsWithUsers(team)
      reqs.map(Json.toJson).map(ApiResult.Data.apply)
  }

  def requestProcess(teamId: TeamId, userId: UserStr, decision: String) = Scoped(_.Team.Lead) { _ ?=> me ?=>
    WithOwnedTeamEnabled(teamId, _.Request): team =>
      api.request(lila.team.TeamRequest.makeId(team.id, userId.id)).flatMap {
        case None      => fuccess(ApiResult.ClientError("No such team join request"))
        case Some(req) => api.processRequest(team, req, decision).inject(ApiResult.Done)
      }
  }

  private val kickLimitReportOnce = scalalib.cache.OnceEvery[UserId](10.minutes)

  def kickUser(teamId: TeamId, username: UserStr) = Scoped(_.Team.Lead) { ctx ?=> me ?=>
    WithOwnedTeamEnabled(teamId, _.Kick): team =>
      def limited =
        if kickLimitReportOnce(username.id) then
          lila
            .log("security")
            .warn(s"API team.kick limited team:${teamId} user:${me.username} ip:${req.ipAddress}")
        fuccess(ApiResult.Limited)
      limit.teamKick(req.ipAddress, limited, cost = if me.isVerified || me.isApiHog then 0 else 1):
        api.kick(team, username.id).inject(ApiResult.Done)
  }

  private def WithOwnedTeamEnabled(teamId: TeamId, perm: TeamSecurity.Permission.Selector)(
      f: TeamModel => Fu[ApiResult]
  )(using me: Me): Fu[Result] =
    api
      .teamEnabled(teamId)
      .flatMap {
        case Some(team) =>
          api
            .isGranted(team.id, me.value, perm)
            .flatMap:
              if _ then f(team)
              else fuccess(ApiResult.ClientError("Insufficient team permissions"))
        case None => fuccess(ApiResult.NoData)
      }
      .map(apiC.toHttp)
