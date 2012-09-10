package lila
package tournament

import akka.actor._
import akka.actor.ReceiveTimeout
import akka.util.duration._
import akka.util.Timeout
import akka.pattern.{ ask, pipe }
import akka.dispatch.{ Future, Promise }
import play.api.libs.concurrent._
import play.api.Play.current
import scalaz.effects._

final class Organizer(
    api: TournamentApi,
    repo: TournamentRepo) extends Actor {

  implicit val timeout = Timeout(1 second)
  implicit val executor = Akka.system.dispatcher

  def receive = {

    case StartTournaments               ⇒ startTournaments.unsafePerformIO
    case StartTournament(tour: Created) ⇒ api.start(tour).unsafePerformIO

    case StartPairings                  ⇒ startPairings.unsafePerformIO
    case StartPairing(tour: Started)    ⇒ startPairing(tour).unsafePerformIO
  }

  def startTournaments = repo.created flatMap { created =>
    (created map api.start).sequence
  } 

  def startPairings = repo.started flatMap { started =>
    (started map startPairing).sequence
  }
  def startPairing(tour: Started) =
    Pairing.createNewPairings(tour.users, tour.pairings).toNel.fold(
      pairings ⇒ api.makePairings(tour, pairings),
      io()
    )

}
