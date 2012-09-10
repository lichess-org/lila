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

final class Organizer(
  api: TournamentApi,
  repo: TournamentRepo) extends Actor {

  implicit val timeout = Timeout(1 second)
  implicit val executor = Akka.system.dispatcher

  def receive = {

    case StartTournament => startTournaments.unsafePerformIO
  }

  def startTournament = for {
    tours <- repo.created
  } yield (tours filter (_.readyToStart) map api.start).sequence



}
