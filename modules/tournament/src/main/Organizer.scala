package lila.tournament

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._
import lila.game.actorApi.FinishGame
import lila.hub.actorApi.map.Tell
import lila.hub.actorApi.WithUserIds
import makeTimeout.short

private[tournament] final class Organizer(
    api: TournamentApi,
    reminder: ActorRef,
    isOnline: String => Boolean,
    socketHub: ActorRef) extends Actor {

  context.system.lilaBus.subscribe(self, 'finishGame)

  def receive = {

    case EnterableTournaments => TournamentRepo.unsortedEnterable foreach {
      _ foreach { tour =>
        tour schedule match {
          case None =>
            if (tour.isEmpty) api wipeEmpty tour
            else if (tour.enoughPlayersToStart) api startIfReady tour
            else ejectLeavers(tour)
          case Some(schedule) =>
            if (schedule.at.isBeforeNow) api startScheduled tour
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

    case FinishGame(game, _, _) =>
      api finishGame game foreach { _ map (_.id) foreach api.socketReload }
  }

  private def ejectLeavers(tour: Created) =
    tour.userIds filterNot isOnline foreach { api.withdraw(tour, _) }

  private def startPairing(tour: Started) {
    withUserIds(tour.id) { ids =>
      (tour.activeUserIds intersect ids) |> { users =>
        Pairing.createNewPairings(users, tour.pairings, tour.nbActiveUsers).toNel foreach { pairings =>
          api.makePairings(tour, pairings)
        }
      }
    }
  }

  private def withUserIds(tourId: String)(f: List[String] => Unit) {
    socketHub ! Tell(tourId, WithUserIds(ids => f(ids.toList)))
  }
}
