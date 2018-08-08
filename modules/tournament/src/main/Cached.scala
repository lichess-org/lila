package lidraughts.tournament

import scala.concurrent.duration._

import lidraughts.memo._
import lidraughts.user.User

private[tournament] final class Cached(
    asyncCache: lidraughts.memo.AsyncCache.Builder,
    createdTtl: FiniteDuration,
    rankingTtl: FiniteDuration
)(implicit system: akka.actor.ActorSystem) {

  val nameCache = new Syncache[String, Option[String]](
    name = "tournament.name",
    compute = id => TournamentRepo byId id map2 { (tour: Tournament) => tour.fullName },
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(1 hour),
    logger = logger
  )

  def name(id: String): Option[String] = nameCache sync id

  val promotable = asyncCache.single(
    name = "tournament.promotable",
    TournamentRepo.promotable,
    expireAfter = _.ExpireAfterWrite(createdTtl)
  )

  def findNext(tour: Tournament): Fu[Option[Tournament]] = tour.perfType ?? { pt =>
    promotable.get map { tours =>
      tours
        .filter(_.isScheduled)
        .filter { t =>
          if (tour.conditions.isRatingLimited) tour.conditions sameRatings t.conditions
          else t.perfType has pt
        }
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
    expireAfter = _.ExpireAfterWrite(3.seconds)
  )

  // only applies to finished tournaments
  private val finishedRanking = asyncCache.multi[String, Ranking](
    name = "tournament.finishedRanking",
    f = PlayerRepo.computeRanking,
    expireAfter = _.ExpireAfterAccess(rankingTtl)
  )

  private[tournament] object sheet {

    import arena.ScoringSystem.Sheet

    private case class SheetKey(tourId: Tournament.ID, userId: User.ID)

    def apply(tour: Tournament, userId: User.ID): Fu[Sheet] =
      cache.get(SheetKey(tour.id, userId))

    def update(tour: Tournament, userId: User.ID): Fu[Sheet] = {
      val key = SheetKey(tour.id, userId)
      cache.refresh(key)
      cache.get(key)
    }

    private def compute(key: SheetKey): Fu[Sheet] =
      PairingRepo.finishedByPlayerChronological(key.tourId, key.userId) map {
        arena.ScoringSystem.sheet(key.userId, _)
      }

    private val cache = asyncCache.multi[SheetKey, Sheet](
      name = "tournament.sheet",
      f = compute,
      expireAfter = _.ExpireAfterAccess(3.minutes)
    )
  }
}
