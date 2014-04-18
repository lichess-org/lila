package lila.team

import lila.memo.{ MixedCache, AsyncCache }
import scala.concurrent.duration._

private[team] final class Cached {

  private val nameCache = MixedCache[String, Option[String]](TeamRepo.name,
    timeToLive = 6 hours,
    default = _ => none)

  def name(id: String) = nameCache get id

  private[team] val teamIdsCache = MixedCache[String, List[String]](MemberRepo.teamIdsByUser,
    timeToLive = 2 hours,
    default = _ => Nil)

  def teamIds(userId: String) = teamIdsCache get userId

  val nbRequests = AsyncCache(
    (userId: String) => TeamRepo teamIdsByCreator userId flatMap RequestRepo.countByTeams,
    maxCapacity = 8192)
}
