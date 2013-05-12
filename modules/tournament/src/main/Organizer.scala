package lila.tournament

import actorApi._
import lila.hub.actorApi.WithSocketUserIds
import lila.hub.actorApi.round.FinishGame
import makeTimeout.short

import akka.actor._
import akka.pattern.{ ask, pipe }

private[tournament] final class Organizer(
    api: TournamentApi,
    reminder: ActorRef,
    socketHub: ActorRef) extends Actor {

  def receive = {

    case CreatedTournaments ⇒ TournamentRepo.created foreach {
      _ foreach { tour ⇒
        if (tour.isEmpty) api wipeEmpty tour
        else if (tour.readyToStart) api startIfReady tour
        else withUserIds(tour.id) { ids ⇒
          (tour.userIds diff ids) foreach { api.withdraw(tour, _) }
        }
      }
    }

    case StartedTournaments ⇒ TournamentRepo.started foreach { tours ⇒
      tours foreach { tour ⇒
        if (tour.readyToFinish) api finish tour
        else startPairing(tour)
      }
      reminder ! RemindTournaments(tours)
    }

    case FinishGame(gameId) ⇒
      api.finishGame(gameId) foreach { _ map (_.id) foreach api.socketReload }

  }

  private def startPairing(tour: Started) {
    withUserIds(tour.id) { ids ⇒
      (tour.activeUserIds intersect ids) |> { users ⇒
        Pairing.createNewPairings(users, tour.pairings, tour.nbActiveUsers).toNel foreach { pairings ⇒
          api.makePairings(tour, pairings)
        }
      }
    }
  }

  private def withUserIds(tourId: String)(f: List[String] ⇒ Unit) {
    socketHub ! WithSocketUserIds(tourId, ids ⇒ f(ids.toList))
  }
}
