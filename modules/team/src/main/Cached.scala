package lila.team

import lila.memo.{ Syncache, AsyncCache }
import scala.concurrent.duration._

private[team] final class Cached(implicit system: akka.actor.ActorSystem) {

  private val nameCache = new Syncache[String, Option[String]](
    name = "team.name",
    compute = TeamRepo.name,
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(30 millis),
    timeToLive = 12 hours,
    logger = logger)

  def name(id: String) = nameCache sync id

  private[team] val teamIdsCache = new Syncache[String, Set[String]](
    name = "team.ids",
    compute = MemberRepo.teamIdsByUser,
    default = _ => Set.empty,
    strategy = Syncache.WaitAfterUptime(30 millis),
    timeToLive = 2 hours,
    logger = logger)

  def teamIds(userId: String) = teamIdsCache sync userId

  val nbRequests = AsyncCache[String, Int](
    name = "team.nbRequests",
    f = userId => TeamRepo teamIdsByCreator userId flatMap RequestRepo.countByTeams,
    maxCapacity = 20000)
}
