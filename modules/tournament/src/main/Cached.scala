package lila.tournament

import play.api.i18n.Lang
import scala.concurrent.duration._

import lila.hub.LightTeam.TeamID
import lila.memo._
import lila.hub.actorApi.team.GetLeaderIds
import lila.memo.CacheApi._
import lila.user.User

final private[tournament] class Cached(
    playerRepo: PlayerRepo,
    pairingRepo: PairingRepo,
    tournamentRepo: TournamentRepo,
    cacheApi: CacheApi,
    scheduler: akka.actor.Scheduler
)(
    implicit
    ec: scala.concurrent.ExecutionContext,
    system: akka.actor.ActorSystem
) {

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

  private[tournament] val featuredInTeamCache =
    cacheApi[TeamID, List[Tournament.ID]](256, "tournament.visibleByTeam") {
      _.expireAfterAccess(30 minutes)
        .buildAsyncFuture { teamId =>
          val max = 5
          lila.common.Bus.ask[Set[User.ID]]("teamGetLeaders") { GetLeaderIds(teamId, _) } flatMap { leaders =>
            tournamentRepo.idsUpcomingByTeam(teamId, leaders, max) flatMap { upcomingIds =>
              (upcomingIds.size < max).?? {
                tournamentRepo.idsFinishedByTeam(teamId, leaders, max - upcomingIds.size)
              } dmap { upcomingIds ::: _ }
            }
          }
        }
    }

  private[tournament] def onJoin(tour: Tournament, by: User, withTeamId: Option[TeamID]) =
    tour.conditions.teamMember.map(_.teamId).ifTrue(tour.createdBy == by.id) orElse
      withTeamId.ifTrue(tour.isTeamBattle) foreach { teamId =>
      scheduler.scheduleOnce(1 second) {
        featuredInTeamCache.invalidate(teamId)
      }
    }

  private[tournament] def onKill(tour: Tournament) =
    scheduler.scheduleOnce(1 second) {
      tour.conditions.teamMember.map(_.teamId) foreach featuredInTeamCache.invalidate
      tour.teamBattle.??(_.teams) foreach featuredInTeamCache.invalidate
    }

  private[tournament] val teamInfo =
    cacheApi[(Tournament.ID, TeamID), Option[TeamBattle.TeamInfo]](16, "tournament.teamInfo") {
      _.expireAfterWrite(5 seconds)
        .maximumSize(64)
        .buildAsyncFuture {
          case (tourId, teamId) =>
            tournamentRepo.teamBattleOf(tourId) flatMap {
              _ ?? { battle =>
                playerRepo.teamInfo(tourId, teamId, battle) dmap some
              }
            }
        }
    }

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

    private case class SheetKey(tourId: Tournament.ID, userId: User.ID, version: Sheet.Version)

    def apply(tour: Tournament, userId: User.ID): Fu[Sheet] =
      cache.get(SheetKey(tour.id, userId, Sheet versionOf tour.startsAt))

    def update(tour: Tournament, userId: User.ID): Fu[Sheet] = {
      val key = SheetKey(tour.id, userId, Sheet versionOf tour.startsAt)
      cache.invalidate(key)
      cache.get(key)
    }

    private def compute(key: SheetKey): Fu[Sheet] =
      pairingRepo.finishedByPlayerChronological(key.tourId, key.userId) map {
        arena.Sheet(key.userId, _, key.version)
      }

    private val cache = cacheApi[SheetKey, Sheet](8192, "tournament.sheet") {
      _.expireAfterAccess(3 minutes)
        .maximumSize(32768)
        .buildAsyncFuture(compute)
    }
  }

  private[tournament] val notableFinishedCache = cacheApi.unit[List[Tournament]] {
    _.refreshAfterWrite(15 seconds)
      .buildAsyncFuture(_ => tournamentRepo.notableFinished(20))
  }
}
