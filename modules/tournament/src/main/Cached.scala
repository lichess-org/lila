package lila.tournament

import scala.concurrent.duration._

import lila.memo._

private[tournament] final class Cached(
    createdTtl: FiniteDuration,
    rankingTtl: FiniteDuration) {

  private val nameCache = MixedCache[String, Option[String]](
    ((id: String) => TournamentRepo byId id map2 { (tour: Tournament) => tour.fullName }),
    timeToLive = 6 hours,
    default = _ => none)

  def name(id: String): Option[String] = nameCache get id

  val promotable = AsyncCache.single(
    TournamentRepo.promotable,
    timeToLive = createdTtl)

  // only applies to ongoing tournaments
  val ongoingRanking = AsyncCache[String, Ranking](
    PlayerRepo.computeRanking,
    timeToLive = 3.seconds)

  // only applies to finished tournaments
  val finishedRanking = AsyncCache[String, Ranking](
    PlayerRepo.computeRanking,
    timeToLive = rankingTtl)
}
