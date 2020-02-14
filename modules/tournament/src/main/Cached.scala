package lila.tournament

import play.api.i18n.Lang
import scala.concurrent.duration._

import lila.memo._
import lila.memo.CacheApi._
import lila.user.User

final private[tournament] class Cached(
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    tournamentRepo: TournamentRepo,
    cacheApi: CacheApi
)(implicit ec: scala.concurrent.ExecutionContext) {

  val nameCache = cacheApi.sync[(Tournament.ID, Lang), Option[String]](
    name = "tournament.name",
    initialCapacity = 32768,
    compute = {
      case (id, lang) => tournamentRepo byId id dmap2 { _.name()(lang) }
    },
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(20 minutes)
  )

  val promotable = cacheApi.unit[List[Tournament]] {
    _.refreshAfterWrite(2 seconds)
      .buildAsyncFuture(_ => tournamentRepo.promotable)
  }

  def ranking(tour: Tournament): Fu[Ranking] =
    if (tour.isFinished) finishedRanking get tour.id
    else ongoingRanking get tour.id

  // only applies to ongoing tournaments
  private val ongoingRanking = cacheApi[Tournament.ID, Ranking](64, "tournament.ongoingRanking") {
    _.expireAfterWrite(3 seconds)
      .buildAsyncFuture(playerRepo.computeRanking)
  }

  // only applies to finished tournaments
  private val finishedRanking = cacheApi[Tournament.ID, Ranking](1024, "tournament.finishedRanking") {
    _.expireAfterAccess(1 hour)
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

    private val cache = cacheApi[SheetKey, Sheet](8192, "tournament.sheet") {
      _.expireAfterAccess(3 minutes)
        .maximumSize(32768)
        .buildAsyncFuture(compute)
    }
  }
}
