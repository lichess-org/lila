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

    case StartTournament ⇒ startTournament.unsafePerformIO

    case StartPairing    ⇒ startPairing.unsafePerformIO
  }

  def startTournament = for {
    tours ← repo.created
  } yield (tours filter (_.readyToStart) map api.start).sequence

  def startPairing = for {
    tours ← repo.started
  } yield (for {
    tour ← tours
  } yield Pairer(tour).toNel.fold(
    pairings ⇒ api.makePairings(tour, pairings),
    io()
  )).toList.sequence

}
