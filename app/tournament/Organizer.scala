package lila
package tournament

import game.DbGame
import round.FinishGame

import akka.actor._
import akka.actor.ReceiveTimeout
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import akka.dispatch.{ Future, Promise }
import play.api.libs.concurrent._
import play.api.Play.current

final class Organizer(
    api: TournamentApi,
    repo: TournamentRepo,
    hubMaster: ActorRef) extends Actor {

  implicit val timeout = Timeout(1 second)
  implicit val executor = Akka.system.dispatcher

  def receive = {

    case StartTournaments               ⇒ startTournaments
    case StartTournament(tour: Created) ⇒ startTournament(tour)

    case StartPairings                  ⇒ startPairings
    case StartPairing(tour: Started)    ⇒ startPairing(tour)

    case FinishGame(gameId: String)     ⇒ finishGame(gameId)
  }

  def finishGame(gameId: String) {
    api.finishGame(gameId).unsafePerformIO
  }

  def startTournaments {
    repo.created.unsafePerformIO foreach startTournament
  }

  def startTournament(tour: Created) {
    (api start tour).unsafePerformIO
  }

  def startPairings {
    repo.started.unsafePerformIO foreach startPairing
  }

  def startPairing(tour: Started) {
    (hubMaster ? GetTournamentUsernames(tour.id)).mapTo[Iterable[String]] onSuccess {
      case usernames ⇒
        (tour.users intersect usernames.toList) |> { users ⇒
          Pairing.createNewPairings(users, tour.pairings).toNel foreach { pairings ⇒
            api.makePairings(tour, pairings).unsafePerformIO
          }
        }
    }
  }
}
