package lila.tournament

import actorApi._
import lila.hub.actorApi.round.FinishGame
import makeTimeout.short

import akka.actor._
import akka.pattern.{ ask, pipe }

private[tournament] final class Organizer(
    api: TournamentApi,
    reminder: ActorRef,
    socketHub: ActorRef) extends Actor {

  def receive = {

    case CreatedTournaments               ⇒ createdTournaments
    case CreatedTournament(tour: Created) ⇒ createdTournament(tour)

    case StartedTournaments               ⇒ startedTournaments
    case StartedTournament(tour: Started) ⇒ startedTournament(tour)

    case StartPairings                    ⇒ startPairings
    case StartPairing(tour: Started)      ⇒ startPairing(tour)

    case FinishGame(gameId) ⇒
      api.finishGame(gameId) foreach { _ map (_.id) foreach api.socketReload }
  }

  def createdTournaments {
    TournamentRepo.created foreach { _ foreach createdTournament }
  }

  def createdTournament(tour: Created) {
    if (tour.isEmpty) api wipeEmpty tour
    else if (tour.readyToStart) api startIfReady tour
    else withUserIds(tour.id) { ids ⇒
      (tour.userIds diff ids) foreach { api.withdraw(tour, _) }
    }
  }

  def startedTournaments {
    TournamentRepo.started foreach { tours ⇒
      tours foreach startedTournament
      reminder ! RemindTournaments(tours)
    }
  }

  def startedTournament(tour: Started) {
    if (tour.readyToFinish) api finish tour
  }

  def startPairings {
    TournamentRepo.started foreach { _ foreach startPairing }
  }

  def startPairing(tour: Started) {
    withUserIds(tour.id) { ids ⇒
      (tour.activeUserIds intersect ids) |> { users ⇒
        Pairing.createNewPairings(users, tour.pairings, tour.nbActiveUsers).toNel foreach { pairings ⇒
          api.makePairings(tour, pairings)
        }
      }
    }
  }

  private def withUserIds(tourId: String)(f: List[String] ⇒ Unit) {
    (socketHub ? GetTournamentUserIds(tourId)).mapTo[Iterable[String]] foreach { ids ⇒
      f(ids.toList)
    }
  }

}
