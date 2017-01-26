package lila.tournament

import scala.concurrent.duration._

import lila.memo._

private[tournament] final class Cached(
    asyncCache: lila.memo.AsyncCache2.Builder,
    createdTtl: FiniteDuration,
    rankingTtl: FiniteDuration)(implicit system: akka.actor.ActorSystem) {

  val nameCache = new Syncache[String, Option[String]](
    name = "tournament.name",
    compute = id => TournamentRepo byId id map2 { (tour: Tournament) => tour.fullName },
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(1 hour),
    logger = logger)

  def name(id: String): Option[String] = nameCache sync id

  val promotable = asyncCache.single(
    name = "tournament.promotable",
    TournamentRepo.promotable,
    expireAfter = _.ExpireAfterWrite(createdTtl))

  def findNext(tour: Tournament): Fu[Option[Tournament]] = tour.perfType ?? { pt =>
    promotable.get map { tours =>
      tours
        .filter(_.perfType contains pt)
        .filter(_.isScheduled)
        .sortBy(_.startsAt)
        .headOption
    }
  }

  def ranking(tour: Tournament): Fu[Ranking] =
    if (tour.isFinished) finishedRanking get tour.id
    else ongoingRanking get tour.id

  // only applies to ongoing tournaments
  private val ongoingRanking = asyncCache.multi[String, Ranking](
    name = "tournament.ongoingRanking",
    f = PlayerRepo.computeRanking,
    expireAfter = _.ExpireAfterWrite(3.seconds))

  // only applies to finished tournaments
  private val finishedRanking = asyncCache.multi[String, Ranking](
    name = "tournament.finishedRanking",
    f = PlayerRepo.computeRanking,
    expireAfter = _.ExpireAfterAccess(rankingTtl))
}
