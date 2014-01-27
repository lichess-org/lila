package lila.tournament

import akka.actor._
import akka.pattern.{ ask, pipe }

import actorApi._
import lila.hub.actorApi.map.Tell
import lila.game.actorApi.FinishGame
import lila.hub.actorApi.WithUserIds
import makeTimeout.short

private[tournament] final class Organizer(
    api: TournamentApi,
    reminder: ActorRef,
    socketHub: ActorRef) extends Actor {

  context.system.lilaBus.subscribe(self, 'finishGame)

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

    case FinishGame(game, _, _) ⇒
      api finishGame game foreach { _ map (_.id) foreach api.socketReload }
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
    socketHub ! Tell(tourId, WithUserIds(ids ⇒ f(ids.toList)))
  }
}
