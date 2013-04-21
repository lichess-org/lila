package lila.app

import lila.socket.actorApi._
import lila.hub.actorApi._

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import play.api.libs.concurrent._
import play.api.Mode

object Cron {

  def start(system: ActorSystem) {

    // implicit val timeout = makeTimeout(500 millis)
    // val actors = Env.hub.actor
    // val sockets = Env.hub.socket

    // message(5 seconds) {
    //   Env.tournament.organizer -> tournament.CreatedTournaments
    // }

    // message(3 seconds) {
    //   Env.tournament.organizer -> tournament.StartedTournaments
    // }

    // message(3 seconds) {
    //   Env.tournament.organizer -> tournament.StartPairings
    // }
  }
}
