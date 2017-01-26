package lila.team

import lila.memo.Syncache
import scala.concurrent.duration._

private[team] final class Cached(
    asyncCache: lila.memo.AsyncCache2.Builder
)(implicit system: akka.actor.ActorSystem) {

  val nameCache = new Syncache[String, Option[String]](
    name = "team.name",
    compute = TeamRepo.name,
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(1 hour),
    logger = logger)

  def name(id: String) = nameCache sync id

  private val teamIdsCache = new Syncache[String, Set[String]](
    name = "team.ids",
    compute = MemberRepo.teamIdsByUser,
    default = _ => Set.empty,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(1 hour),
    logger = logger)

  def syncTeamIds = teamIdsCache sync _
  def teamIds = teamIdsCache async _

  def invalidateTeamIds = teamIdsCache invalidate _

  val nbRequests = asyncCache.clearable[lila.user.User.ID, Int](
    name = "team.nbRequests",
    f = userId => TeamRepo teamIdsByCreator userId flatMap RequestRepo.countByTeams,
    expireAfter = _.ExpireAfterAccess(12 minutes))
}
