package lila.tournament

import scala.concurrent.duration._

import lila.memo._
import lila.user.User

final private[tournament] class Cached(
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    tournamentRepo: TournamentRepo,
    asyncCache: lila.memo.AsyncCache.Builder
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

  private val createdTtl = 2 seconds
  private val rankingTtl = 1 hour

  val nameCache = new Syncache[Tournament.ID, Option[String]](
    name = "tournament.name",
    compute = id =>
      tournamentRepo byId id dmap2 { _.fullName },
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(1 hour),
    logger = logger
  )

  val promotable = asyncCache.single(
    name = "tournament.promotable",
    tournamentRepo.promotable,
    expireAfter = _.ExpireAfterWrite(createdTtl)
  )

  def ranking(tour: Tournament): Fu[Ranking] =
    if (tour.isFinished) finishedRanking get tour.id
    else ongoingRanking get tour.id

  // only applies to ongoing tournaments
  private val ongoingRanking = asyncCache.multi[Tournament.ID, Ranking](
    name = "tournament.ongoingRanking",
    f = playerRepo.computeRanking,
    expireAfter = _.ExpireAfterWrite(3.seconds)
  )

  // only applies to finished tournaments
  private val finishedRanking = asyncCache.multi[Tournament.ID, Ranking](
    name = "tournament.finishedRanking",
    f = playerRepo.computeRanking,
    expireAfter = _.ExpireAfterAccess(rankingTtl)
  )

  private[tournament] object sheet {

    import arena.Sheet

    private case class SheetKey(tourId: Tournament.ID, userId: User.ID)

    def apply(tour: Tournament, userId: User.ID): Fu[Sheet] =
      cache.get(SheetKey(tour.id, userId))

    def update(tour: Tournament, userId: User.ID): Fu[Sheet] = {
      val key = SheetKey(tour.id, userId)
      cache.refresh(key)
      cache.get(key)
    }

    private def compute(key: SheetKey): Fu[Sheet] =
      pairingRepo.finishedByPlayerChronological(key.tourId, key.userId) map {
        arena.Sheet(key.userId, _)
      }

    private val cache = asyncCache.multi[SheetKey, Sheet](
      name = "tournament.sheet",
      f = compute,
      expireAfter = _.ExpireAfterAccess(3.minutes)
    )
  }
}
