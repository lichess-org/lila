package lila.tournament

import scala.concurrent.duration._

import lila.memo._
import lila.memo.CacheApi._
import lila.user.User

final private[tournament] class Cached(
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    tournamentRepo: TournamentRepo,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem) {

  private val createdTtl = 2 seconds

  val nameCache = new Syncache[Tournament.ID, Option[String]](
    name = "tournament.name",
    initialCapacity = 8192,
    compute = id => tournamentRepo byId id dmap2 { _.fullName },
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(1 hour),
    logger = logger
  )

  val promotable = cacheApi.unit[List[Tournament]] {
    _.refreshAfterWrite(createdTtl)
      .buildAsyncFuture(_ => tournamentRepo.promotable)
  }

  def ranking(tour: Tournament): Fu[Ranking] =
    if (tour.isFinished) finishedRanking get tour.id
    else ongoingRanking get tour.id

  // only applies to ongoing tournaments
  private val ongoingRanking = cacheApi[Tournament.ID, Ranking]("tournament.ongoingRanking") {
    _.initialCapacity(64)
      .expireAfterWrite(3 seconds)
      .buildAsyncFuture(playerRepo.computeRanking)
  }

  // only applies to finished tournaments
  private val finishedRanking = cacheApi[Tournament.ID, Ranking]("tournament.finishedRanking") {
    _.initialCapacity(1024)
      .expireAfterAccess(1 hour)
      .maximumSize(2048)
      .buildAsyncFuture(playerRepo.computeRanking)
  }

  private[tournament] object sheet {

    import arena.Sheet

    private case class SheetKey(tourId: Tournament.ID, userId: User.ID)

    def apply(tour: Tournament, userId: User.ID): Fu[Sheet] =
      cache.get(SheetKey(tour.id, userId))

    def update(tour: Tournament, userId: User.ID): Fu[Sheet] = {
      val key = SheetKey(tour.id, userId)
      cache.invalidate(key)
      cache.get(key)
    }

    private def compute(key: SheetKey): Fu[Sheet] =
      pairingRepo.finishedByPlayerChronological(key.tourId, key.userId) map {
        arena.Sheet(key.userId, _)
      }

    private val cache = cacheApi[SheetKey, Sheet]("tournament.sheet") {
      _.initialCapacity(8192)
        .expireAfterAccess(3 minutes)
        .maximumSize(32768)
        .buildAsyncFuture(compute)
    }
  }
}
