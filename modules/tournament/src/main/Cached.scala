package lila.tournament

import play.api.i18n.Lang
import scala.concurrent.duration._

import lila.hub.LightTeam.TeamID
import lila.memo._
import lila.memo.CacheApi._
import lila.user.User

final private[tournament] class Cached(
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    tournamentRepo: TournamentRepo,
    cacheApi: CacheApi
)(implicit
    ec: scala.concurrent.ExecutionContext
) {

  object tourCache {

    private val cache = cacheApi[Tournament.ID, Option[Tournament]](512, "tournament.tournament") {
      _.expireAfterWrite(1 second)
        .buildAsyncFuture(tournamentRepo.byId)
    }
    def clear(id: Tournament.ID) = cache.invalidate(id)

    def byId                         = cache.get _
    def created(id: Tournament.ID)   = cache.get(id).dmap(_.filter(_.isCreated))
    def started(id: Tournament.ID)   = cache.get(id).dmap(_.filter(_.isStarted))
    def enterable(id: Tournament.ID) = cache.get(id).dmap(_.filter(_.isEnterable))
  }

  val nameCache = cacheApi.sync[(Tournament.ID, Lang), Option[String]](
    name = "tournament.name",
    initialCapacity = 65536,
    compute = { case (id, lang) =>
      tournamentRepo byId id dmap2 { _.name()(lang) }
    },
    default = _ => none,
    strategy = Syncache.WaitAfterUptime(20 millis),
    expireAfter = Syncache.ExpireAfterAccess(20 minutes)
  )

  val onHomepage = cacheApi.unit[List[Tournament]] {
    _.refreshAfterWrite(2 seconds)
      .buildAsyncFuture(_ => tournamentRepo.onHomepage)
  }

  def ranking(tour: Tournament): Fu[FullRanking] =
    if (tour.isFinished) finishedRanking get tour.id
    else ongoingRanking get tour.id

  // only applies to ongoing tournaments
  private val ongoingRanking = cacheApi[Tournament.ID, FullRanking](64, "tournament.ongoingRanking") {
    _.expireAfterWrite(3 seconds)
      .buildAsyncFuture(playerRepo.computeRanking)
  }

  // only applies to finished tournaments
  private val finishedRanking = cacheApi[Tournament.ID, FullRanking](1024, "tournament.finishedRanking") {
    _.expireAfterAccess(1 hour)
      .maximumSize(2048)
      .buildAsyncFuture(playerRepo.computeRanking)
  }

  private[tournament] val teamInfo =
    cacheApi[(Tournament.ID, TeamID), Option[TeamBattle.TeamInfo]](16, "tournament.teamInfo") {
      _.expireAfterWrite(5 seconds)
        .maximumSize(64)
        .buildAsyncFuture { case (tourId, teamId) =>
          playerRepo.teamInfo(tourId, teamId) dmap some
        }
    }

  object battle {

    val teamStanding =
      cacheApi[Tournament.ID, List[TeamBattle.RankedTeam]](8, "tournament.teamStanding") {
        _.expireAfterWrite(1 second)
          .buildAsyncFuture { id =>
            tournamentRepo teamBattleOf id flatMap {
              _ ?? { playerRepo.bestTeamIdsByTour(id, _) }
            }
          }
      }
  }

  private[tournament] object sheet {

    import arena.Sheet

    private case class SheetKey(
        tourId: Tournament.ID,
        userId: User.ID,
        version: Sheet.Version,
        streakable: Sheet.Streakable
    )

    def apply(tour: Tournament, userId: User.ID): Fu[Sheet] =
      cache.get(keyOf(tour, userId))

    /* This is not thread-safe! But only called from within a tournament sequencer. */
    def addResult(tour: Tournament, userId: String, pairing: Pairing): Fu[Sheet] = {
      val key = keyOf(tour, userId)
      cache.getIfPresent(key).fold(recompute(tour, userId)) { prev =>
        val next =
          prev map { _.addResult(userId, pairing, Sheet.Version.V2, Sheet.Streakable(tour.streakable)) }
        cache.put(key, next)
        next
      }
    }

    def recompute(tour: Tournament, userId: User.ID): Fu[Sheet] = {
      val key = keyOf(tour, userId)
      cache.invalidate(key)
      cache.get(key)
    }

    private def keyOf(tour: Tournament, userId: User.ID) =
      SheetKey(
        tour.id,
        userId,
        Sheet.Version.of(tour.startsAt),
        Sheet.Streakable(tour.streakable)
      )

    private def compute(key: SheetKey): Fu[Sheet] =
      pairingRepo.finishedByPlayerChronological(key.tourId, key.userId) map {
        arena.Sheet.buildFromScratch(key.userId, _, key.version, key.streakable)
      }

    private val cache = cacheApi[SheetKey, Sheet](32768, "tournament.sheet") {
      _.expireAfterAccess(4 minutes)
        .maximumSize(65536)
        .buildAsyncFuture(compute)
    }
  }

  private[tournament] val notableFinishedCache = cacheApi.unit[List[Tournament]] {
    _.refreshAfterWrite(15 seconds)
      .buildAsyncFuture(_ => tournamentRepo.notableFinished(20))
  }
}
