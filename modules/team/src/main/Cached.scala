package lila.team

import reactivemongo.api.bson.BSONNull
import reactivemongo.api.ReadPreference
import scala.concurrent.duration._

import lila.db.dsl._
import lila.memo.Syncache
import lila.user.User

final class Cached(
    teamRepo: TeamRepo,
    memberRepo: MemberRepo,
    requestRepo: RequestRepo,
    cacheApi: lila.memo.CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  val nameCache = cacheApi.sync[String, Option[String]](
    name = "team.name",
    initialCapacity = 32768,
    compute = teamRepo.name,
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(10 minutes)
  )

  def blockingTeamName(id: Team.ID) = nameCache sync id

  def preloadSet = nameCache preloadSet _

  private val teamIdsCache = cacheApi.sync[User.ID, Team.IdsStr](
    name = "team.ids",
    initialCapacity = 131072,
    compute = u =>
      memberRepo.coll
        .aggregateOne(readPreference = ReadPreference.secondaryPreferred) { framework =>
          import framework._
          Match($doc("_id" $startsWith s"$u@")) -> List(
            Project($doc("_id" -> $doc("$substr" -> $arr("$_id", u.size + 1, -1)))),
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
        }
        .map { doc =>
          Team.IdsStr(~doc.flatMap(_.getAsOpt[List[Team.ID]]("ids")))
        },
    default = _ => Team.IdsStr.empty,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterWrite(40 minutes)
  )

  def syncTeamIds                  = teamIdsCache sync _
  def teamIds                      = teamIdsCache async _
  def teamIdsList(userId: User.ID) = teamIds(userId).dmap(_.toList)
  def teamIdsSet(userId: User.ID)  = teamIds(userId).dmap(_.toSet)

  def invalidateTeamIds = teamIdsCache invalidate _

  val nbRequests = cacheApi[User.ID, Int](32768, "team.nbRequests") {
    _.expireAfterAccess(40 minutes)
      .maximumSize(131072)
      .buildAsyncFuture[User.ID, Int] { userId =>
        teamIds(userId) flatMap { ids =>
          ids.value.nonEmpty ?? teamRepo.countRequestsOfLeader(userId, requestRepo.coll)
        }
      }
  }

  val leaders = cacheApi[Team.ID, Set[User.ID]](128, "team.leaders") {
    _.expireAfterWrite(1 minute)
      .buildAsyncFuture(teamRepo.leadersOf)
  }

  def isLeader(teamId: Team.ID, userId: User.ID): Fu[Boolean] =
    leaders.get(teamId).dmap(_ contains userId)

  val forumAccess = cacheApi[Team.ID, Team.Access](4096, "team.forum.access") {
    _.expireAfterWrite(5 minutes)
      .buildAsyncFuture(id => teamRepo.forumAccess(id).dmap(_ | Team.Access.NONE))
  }

  val unsubs = cacheApi[Team.ID, Int](512, "team.unsubs") {
    _.expireAfterWrite(1 hour).buildAsyncFuture(id => memberRepo.countUnsub(id))
  }
}
