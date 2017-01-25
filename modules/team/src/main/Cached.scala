package lila.team

import lila.memo.{ Syncache, MixedCache, AsyncCache }
import scala.concurrent.duration._

private[team] final class Cached(implicit system: akka.actor.ActorSystem) {

  private val nameCache = new Syncache[String, Option[String]](
    name = "team.name",
    compute = TeamRepo.name,
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(30 millis),
    timeToLive = 12 hours,
    logger = logger)

  def name(id: String) = nameCache get id

  private[team] val teamIdsCache = MixedCache[String, Set[String]](
    name = "team.ids",
    MemberRepo.teamIdsByUser,
    timeToLive = 2 hours,
    default = _ => Set.empty,
    logger = logger)

  def teamIds(userId: String) = teamIdsCache get userId

  val nbRequests = AsyncCache[String, Int](
    name = "team.nbRequests",
    f = userId => TeamRepo teamIdsByCreator userId flatMap RequestRepo.countByTeams,
    maxCapacity = 20000)
}
