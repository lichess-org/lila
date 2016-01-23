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

  def ranking(tour: Tournament): Fu[Ranking] =
    if (tour.isFinished) finishedRanking(tour.id)
    else ongoingRanking(tour.id)

  // only applies to ongoing tournaments
  private val ongoingRanking = AsyncCache[String, Ranking](
    tourId => PlayerRepo computeRanking tourId map {
      case (ranking, leaderIdOption) =>
        leaderIdOption foreach {
          TournamentRepo.setLeaderId(tourId, _)
        }
        ranking
    },
    timeToLive = 3.seconds)

  // only applies to finished tournaments
  private val finishedRanking = AsyncCache[String, Ranking](
    tourId => PlayerRepo computeRanking tourId map (_._1),
    timeToLive = rankingTtl)
}
