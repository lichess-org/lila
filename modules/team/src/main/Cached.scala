package lila.team

import lila.memo.{ Syncache, AsyncCache }
import scala.concurrent.duration._

private[team] final class Cached(implicit system: akka.actor.ActorSystem) {

  val nameCache = new Syncache[String, Option[String]](
    name = "team.name",
    compute = TeamRepo.name,
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    timeToLive = 12 hours,
    logger = logger)

  def name(id: String) = nameCache sync id

  private val teamIdsCache = new Syncache[String, Set[String]](
    name = "team.ids",
    compute = MemberRepo.teamIdsByUser,
    default = _ => Set.empty,
    strategy = Syncache.WaitAfterUptime(20 millis),
    timeToLive = 2 hours,
    logger = logger)

  def syncTeamIds = teamIdsCache sync _
  def teamIds = teamIdsCache async _

  def invalidateTeamIds = teamIdsCache invalidate _

  val nbRequests = AsyncCache[String, Int](
    name = "team.nbRequests",
    f = userId => TeamRepo teamIdsByCreator userId flatMap RequestRepo.countByTeams,
    maxCapacity = 20000)
}
