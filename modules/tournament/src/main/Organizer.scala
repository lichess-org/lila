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
          case None =>
            if (tour.isEmpty) api wipeEmpty tour
            else if (tour.hasWaitedEnough) api startOrDelete tour
            else ejectLeavers(tour)
          case Some(schedule) =>
            if (tour.hasWaitedEnough) api startScheduled tour
            else ejectLeavers(tour)
        }
      }
    }

    case StartedTournaments => TournamentRepo.started foreach { tours =>
      tours foreach { tour =>
        if (tour.readyToFinish) api finish tour
        else startPairing(tour)
      }
      reminder ! RemindTournaments(tours)
    }

    case FinishGame(game, _, _)                    => api finishGame game

    case lila.hub.actorApi.mod.MarkCheater(userId) => api ejectCheater userId
  }

  private def ejectLeavers(tour: Created) =
    tour.userIds filterNot isOnline foreach { api.withdraw(tour, _) }

  private def startPairing(tour: Started) {
    if (!tour.isAlmostFinished) getUserIds(tour) foreach { res =>
      val allUsers = res.copy(
        all = tour.activeUserIds intersect res.all,
        waiting = tour.activeUserIds intersect res.waiting)
      tour.system.pairingSystem.createPairings(tour, allUsers) onSuccess {
        case (pairings, events) =>
          pairings.toNel foreach { api.makePairings(tour, _, events) }
      }
    }
  }

  private def getUserIds(tour: Tournament): Fu[AllUserIds] =
    socketHub ? Ask(tour.id, GetAllUserIds) mapTo manifest[AllUserIds]
}
