package lila.team

import reactivemongo.api.bson.BSONNull

import lila.db.dsl.{ *, given }
import lila.memo.Syncache
import lila.hub.LightTeam.TeamName

final class Cached(
    teamRepo: TeamRepo,
    memberRepo: TeamMemberRepo,
    requestRepo: TeamRequestRepo,
    cacheApi: lila.memo.CacheApi
)(using Executor):

  val nameCache = cacheApi.sync[TeamId, Option[TeamName]](
    name = "team.name",
    initialCapacity = 32768,
    compute = teamRepo.name,
    default = _ => none,
    strategy = Syncache.Strategy.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfter.Access(10 minutes)
  )

  export nameCache.{ preloadSet, sync as blockingTeamName }

  private val teamIdsCache = cacheApi.sync[UserId, Team.IdsStr](
    name = "team.ids",
    initialCapacity = 131_072,
    compute = u =>
      memberRepo.coll
        .aggregateOne(_.sec): framework =>
          import framework.*
          Match($doc("_id" $startsWith s"$u@")) -> List(
            Project($doc("_id" -> $doc("$substr" -> $arr("$_id", u.value.size + 1, -1)))),
            PipelineOperator(
              $lookup.pipeline(
                from = teamRepo.coll,
                as = "team",
                local = "_id",
                foreign = "_id",
                pipe = List(
                  $doc("$match"   -> $doc("enabled" -> true)),
                  $doc("$project" -> $id(true))
                )
              )
            ),
            UnwindField("team"),
            ReplaceRootField("team"),
            Group(BSONNull)("ids" -> PushField("_id"))
          )
        .map: doc =>
          Team.IdsStr(~doc.flatMap(_.getAsOpt[List[TeamId]]("ids"))),
    default = _ => Team.IdsStr.empty,
    strategy = Syncache.Strategy.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfter.Write(40 minutes)
  )

  export teamIdsCache.{ async as teamIds, invalidate as invalidateTeamIds, sync as syncTeamIds }
  def teamIdsList[U: UserIdOf](user: U): Fu[List[TeamId]]    = teamIds(user.id).dmap(_.toList)
  def teamIdsSet[U: UserIdOf](user: UserId): Fu[Set[TeamId]] = teamIds(user.id).dmap(_.toSet)

  val nbRequests = cacheApi[UserId, Int](32_768, "team.nbRequests"):
    _.expireAfterAccess(40 minutes)
      .maximumSize(131_072)
      .buildAsyncFuture[UserId, Int]: userId =>
        for
          myTeams     <- teamIds(userId)
          leaderTeams <- myTeams.nonEmpty so memberRepo.teamsWhereIsGrantedRequest(userId)
          nbReqs      <- requestRepo.countPendingForTeams(leaderTeams)
        yield nbReqs

  val forumAccess = cacheApi[TeamId, Team.Access](1024, "team.forum.access"):
    _.expireAfterWrite(5 minutes).buildAsyncFuture(id => teamRepo.forumAccess(id).dmap(_ | Team.Access.NONE))

  val unsubs = cacheApi[TeamId, Int](512, "team.unsubs"):
    _.expireAfterWrite(1 hour).buildAsyncFuture(memberRepo.countUnsub)
