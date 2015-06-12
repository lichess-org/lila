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

  context.system.lilaBus.subscribe(self, 'finishGame, 'adjustCheater)

  def receive = {

    case AllCreatedTournaments => TournamentRepo.allCreated foreach {
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

    case StartedTournaments => TournamentRepo.started foreach {
      _ foreach { tour =>
        PlayerRepo activeUserIds tour.id foreach { activeUserIds =>
          if (tour.secondsToFinish == 0) api finish tour
          else if (!tour.scheduled && activeUserIds.size < 2) api finish tour
          else if (!tour.isAlmostFinished) startPairing(tour, activeUserIds)
          reminder ! RemindTournament(tour, activeUserIds)
        }
      }
    }

    case FinishGame(game, _, _)                    => api finishGame game

    case lila.hub.actorApi.mod.MarkCheater(userId) => api ejectCheater userId
  }

  private def ejectLeavers(tour: Tournament) =
    PlayerRepo userIds tour.id foreach {
      _ filterNot isOnline foreach { api.withdraw(tour.id, _) }
    }

  private def startPairing(tour: Tournament, activeUserIds: List[String]) = for {
    socketUserIds <- getSocketUserIds(tour)
    allUsers = socketUserIds.copy(
      all = activeUserIds intersect socketUserIds.all,
      waiting = activeUserIds intersect socketUserIds.waiting)
  } {
    tour.system.pairingSystem.createPairings(tour, allUsers) onSuccess {
      case (pairings, events) =>
        pairings.toNel foreach { api.makePairings(tour, _, events) }
    }
  }

  private def getSocketUserIds(tour: Tournament): Fu[AllUserIds] =
    socketHub ? Ask(tour.id, GetAllUserIds) mapTo manifest[AllUserIds]
}
