package lila.team

import com.softwaremill.tagging._
import scala.concurrent.duration._

import lila.memo.Syncache
import lila.user.User

final class Cached(
    teamRepo: TeamRepo,
    memberRepo: MemberRepo,
    requestRepo: RequestRepo @@ NewRequest,
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
    compute = u =>
      for {
        teamIds <- memberRepo.teamIdsByUser(u)
        enabled <- teamRepo.filterEnabled(teamIds take 100)
      } yield Team.IdsStr(enabled),
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
}
