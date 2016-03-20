package lila.tournament

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._

import actorApi._
import lila.game.actorApi.FinishGame
import lila.hub.actorApi.map.Ask
import lila.hub.actorApi.WithUserIds
import makeTimeout.short

private[tournament] final class Organizer(
    api: TournamentApi,
    reminder: ActorRef,
    isOnline: String => Boolean,
    socketHub: ActorRef) extends Actor {

  context.system.lilaBus.subscribe(self, 'finishGame, 'adjustCheater, 'adjustBooster)

  override def preStart {
    context.system.scheduler.scheduleOnce(5 seconds, self, AllCreatedTournaments)
    context.system.scheduler.scheduleOnce(6 seconds, self, StartedTournaments)
  }

  def receive = {

    case AllCreatedTournaments => TournamentRepo allCreated 30 map { tours =>
      tours foreach { tour =>
        tour.schedule match {
          case None => PlayerRepo count tour.id foreach {
            case 0 => api wipe tour
            case nb if tour.hasWaitedEnough =>
              if (nb >= Tournament.minPlayers) api start tour
              else api wipe tour
            case _ =>
          }
          case Some(schedule) if tour.hasWaitedEnough => api start tour
          case _                                      => ejectLeavers(tour)
        }
      }
      lila.mon.tournament.created(tours.size)
    } andThenAnyway {
      context.system.scheduler.scheduleOnce(2 seconds, self, AllCreatedTournaments)
    }

    case StartedTournaments =>
      val startAt = nowMillis
      TournamentRepo.started.flatMap { started =>
        lila.common.Future.traverseSequentially(started) { tour =>
          PlayerRepo activeUserIds tour.id flatMap { activeUserIds =>
            val nb = activeUserIds.size
            val result: Funit =
              if (tour.secondsToFinish == 0) fuccess(api finish tour)
              else if (!tour.scheduled && nb < 2) fuccess(api finish tour)
              else if (!tour.isAlmostFinished) startPairing(tour, activeUserIds, startAt)
              else funit
            result >>- {
              reminder ! RemindTournament(tour, activeUserIds)
            } inject nb
          }
        }.addEffect { playerCounts =>
          lila.mon.tournament.player(playerCounts.sum.pp("player count"))
          lila.mon.tournament.started(started.size.pp("started count"))
        }
      } andThenAnyway {
        context.system.scheduler.scheduleOnce(3 seconds, self, StartedTournaments)
      }

    case FinishGame(game, _, _)                          => api finishGame game

    case lila.hub.actorApi.mod.MarkCheater(userId)       => api ejectLame userId

    case lila.hub.actorApi.mod.MarkBooster(userId)       => api ejectLame userId

    case lila.hub.actorApi.round.Berserk(gameId, userId) => api.berserk(gameId, userId)
  }

  private def ejectLeavers(tour: Tournament) =
    PlayerRepo userIds tour.id foreach {
      _ filterNot isOnline foreach { api.withdraw(tour.id, _) }
    }

  private def startPairing(tour: Tournament, activeUserIds: List[String], startAt: Long): Funit =
    getWaitingUsers(tour) zip PairingRepo.playingUserIds(tour) map {
      case (waitingUsers, playingUserIds) =>
        val users = waitingUsers intersect activeUserIds diff playingUserIds
        api.makePairings(tour, users, startAt)
    }

  private def getWaitingUsers(tour: Tournament): Fu[WaitingUsers] =
    socketHub ? Ask(tour.id, GetWaitingUsers) mapTo manifest[WaitingUsers]
}
