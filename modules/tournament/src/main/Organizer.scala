package lila.tournament

import akka.actor._
import akka.pattern.{ ask, pipe }

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

  def receive = {

    case AllCreatedTournaments => TournamentRepo allCreated 30 foreach {
      _ foreach { tour =>
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
    }

    case StartedTournaments =>
      val startAt = nowMillis
      var totalPlayers = 0
      TournamentRepo.started foreach {
        _ foreach { tour =>
          PlayerRepo activeUserIds tour.id foreach { activeUserIds =>
            val nb = activeUserIds.size
            if (tour.secondsToFinish == 0) api finish tour
            else if (!tour.scheduled && nb < 2) api finish tour
            else if (!tour.isAlmostFinished) startPairing(tour, activeUserIds, startAt)
            reminder ! RemindTournament(tour, activeUserIds)
            totalPlayers = totalPlayers + nb
          }
        }
      }
      kamon.Kamon.metrics.histogram("tournament.players") record totalPlayers

    case FinishGame(game, _, _)                          => api finishGame game

    case lila.hub.actorApi.mod.MarkCheater(userId)       => api ejectLame userId

    case lila.hub.actorApi.mod.MarkBooster(userId)       => api ejectLame userId

    case lila.hub.actorApi.round.Berserk(gameId, userId) => api.berserk(gameId, userId)
  }

  private def ejectLeavers(tour: Tournament) =
    PlayerRepo userIds tour.id foreach {
      _ filterNot isOnline foreach { api.withdraw(tour.id, _) }
    }

  private def startPairing(tour: Tournament, activeUserIds: List[String], startAt: Long) =
    getWaitingUsers(tour) zip PairingRepo.playingUserIds(tour) foreach {
      case (waitingUsers, playingUserIds) =>
        val users = waitingUsers intersect activeUserIds diff playingUserIds
        api.makePairings(tour, users, startAt)
    }

  private def getWaitingUsers(tour: Tournament): Fu[WaitingUsers] =
    socketHub ? Ask(tour.id, GetWaitingUsers) mapTo manifest[WaitingUsers]
}
