package lila
package tournament

import game.DbGame
import round.FinishGame

import akka.actor._
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import akka.dispatch.{ Future, Promise }
import play.api.libs.concurrent._
import play.api.Play.current

private[tournament] final class Organizer(
    api: TournamentApi,
    repo: TournamentRepo,
    reminder: ActorRef,
    register: ActorRef,
    hubMaster: ActorRef) extends Actor {

  implicit val timeout = Timeout(1 second)

  def receive = {

    case CreatedTournaments               ⇒ createdTournaments
    case CreatedTournament(tour: Created) ⇒ createdTournament(tour)

    case StartedTournaments               ⇒ startedTournaments
    case StartedTournament(tour: Started) ⇒ startedTournament(tour)

    case StartPairings                    ⇒ startPairings
    case StartPairing(tour: Started)      ⇒ startPairing(tour)

    case FinishGame(game)                 ⇒ finishGame(game)
  }

  def finishGame(game: DbGame) {
    api.finishGame(game).unsafePerformIO foreach { tour ⇒
      hubMaster ! Forward(tour.id, Reload)
    }
  }

  def createdTournaments {
    repo.created.unsafePerformIO foreach createdTournament
  }

  def createdTournament(tour: Created) {
    if (tour.isEmpty) (api wipeEmpty tour).unsafePerformIO
    else if (tour.readyToStart) api start tour map (_.unsafePerformIO) foreach { reminder ! _ }
    else (hubMaster ? GetTournamentUsernames(tour.id)).mapTo[Iterable[String]] onSuccess {
      case usernames ⇒ (tour.userIds diff usernames.toList.map(_.toLowerCase)) |> { leavers ⇒
        leavers.map(u ⇒ api.withdraw(tour, u)).sequence.unsafePerformIO
      }
    }
  }

  def startedTournaments {
    repo.started.unsafePerformIO ~ { tours ⇒
      tours foreach startedTournament
      register ! SetTournaments(tours)
    }
  }

  def startedTournament(tour: Started) {
    if (tour.readyToFinish) (api finish tour).unsafePerformIO
  }

  def startPairings {
    repo.started.unsafePerformIO foreach startPairing
  }

  def startPairing(tour: Started) {
    (hubMaster ? GetTournamentUsernames(tour.id)).mapTo[Iterable[String]] onSuccess {
      case usernames ⇒
        (tour.activeUserIds intersect usernames.toList.map(_.toLowerCase)) |> { users ⇒
          Pairing.createNewPairings(users, tour.pairings, tour.nbActiveUsers).toNel foreach { pairings ⇒
            api.makePairings(tour, pairings).unsafePerformIO
          }
        }
    }
  }
}
