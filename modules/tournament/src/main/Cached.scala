package lila.tournament

import scala.concurrent.duration._

import lila.memo._

private[tournament] final class Cached(
    createdTtl: FiniteDuration,
    rankingTtl: FiniteDuration)(implicit system: akka.actor.ActorSystem) {

  private val nameCache = new Syncache[String, Option[String]](
    name = "tournament.name",
    compute = id => TournamentRepo byId id map2 { (tour: Tournament) => tour.fullName },
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(50 millis),
    timeToLive = 6 hours,
    logger = logger)

  def name(id: String): Option[String] = nameCache get id

  val promotable = AsyncCache.single(
    name = "tournament.promotable",
    TournamentRepo.promotable,
    timeToLive = createdTtl)

  def findNext(tour: Tournament): Fu[Option[Tournament]] = tour.perfType ?? { pt =>
    promotable(true) map { tours =>
      tours
        .filter(_.perfType contains pt)
        .filter(_.isScheduled)
        .sortBy(_.startsAt)
        .headOption
    }
  }

  def ranking(tour: Tournament): Fu[Ranking] =
    if (tour.isFinished) finishedRanking(tour.id)
    else ongoingRanking(tour.id)

  // only applies to ongoing tournaments
  private val ongoingRanking = AsyncCache[String, Ranking](
    name = "tournament.ongoingRanking",
    f = PlayerRepo.computeRanking,
    timeToLive = 3.seconds)

  // only applies to finished tournaments
  private val finishedRanking = AsyncCache[String, Ranking](
    name = "tournament.finishedRanking",
    f = PlayerRepo.computeRanking,
    timeToLive = rankingTtl)
}
