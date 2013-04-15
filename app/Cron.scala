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

    loginfo("[boot] cron (" + Env.api.mode + ")")

    // implicit val timeout = makeTimeout(500 millis)
    // val actors = Env.hub.actor
    // val sockets = Env.hub.socket



    // if (current.mode != Mode.Dev) {

    //   Env.ai.clientDiagnose

    //   effect(4.5 hours, "game: cleanup") {
    //     Env.titivate.cleanupUnplayed flatMap { _ ⇒
    //       Env.titivate.cleanupNext
    //     }
    //   }

    //   effect(1.13 hour, "game: finish by clock") {
    //     Env.titivate.finishByClock
    //   }

    //   effect(2.3 hour, "game: finish abandoned") {
    //     Env.titivate.finishAbandoned
    //   }
    // }

    // unsafe(10 seconds, "ai: diagnose") {
    //   Env.ai.clientDiagnose
    // }

    // effect(5 minutes, "search: index finished games") {
    //   Env.search.indexer.indexQueue
    // }

    // effect(2 hours, "search: optimize index") {
    //   Env.search.indexer.optimize
    // }

    // unsafe(10 minutes, "firewall: refresh") {
    //   Env.security.firewall.refresh
    // }

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
