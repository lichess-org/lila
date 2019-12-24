package lila.team

import scala.concurrent.duration._

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
    initialCapacity = 4096,
    compute = teamRepo.name,
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(20 minutes)
  )

  def blockingTeamName(id: Team.ID) = nameCache sync id

  def preloadSet = nameCache preloadSet _

  private val teamIdsCache = cacheApi.sync[User.ID, Team.IdsStr](
    name = "team.ids",
    initialCapacity = 65536,
    compute = u => memberRepo.teamIdsByUser(u).dmap(Team.IdsStr.apply),
    default = _ => Team.IdsStr.empty,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterWrite(1 hour)
  )

  def syncTeamIds                  = teamIdsCache sync _
  def teamIds                      = teamIdsCache async _
  def teamIdsList(userId: User.ID) = teamIds(userId).dmap(_.toList)

  def invalidateTeamIds = teamIdsCache invalidate _

  val nbRequests = cacheApi[User.ID, Int](32768, "team.nbRequests") {
    _.expireAfterAccess(25 minutes)
      .maximumSize(65536)
      .buildAsyncFuture[User.ID, Int] { userId =>
        teamRepo teamIdsByCreator userId flatMap requestRepo.countByTeams,
      }
  }
}
