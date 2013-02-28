package lila.app
package core

import akka.actor.ActorRef
import akka.pattern.{ ask, pipe }
import akka.util.Timeout
import scala.concurrent.duration._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.concurrent._
import play.api.Mode
import scalaz.effects._

object Cron {

  def start(env: CoreEnv) {

    implicit val current = env.app
    implicit val timeout = Timeout(500 millis)
    implicit val executor = Akka.system.dispatcher

    println("Start cron mode " + current.mode)

    unsafe(5 seconds, "meta hub: broom") {
      env.metaHub ! socket.Broom
    }

    message(1 seconds) {
      env.monitor.reporting -> monitor.Update(env)
    }

    message(1 second) {
      env.lobby.hub -> lobby.WithHooks(env.lobby.hookMemo.putAll)
    }

    unsafe(2 seconds, "meta hub: refresh") {
      env.metaHub.?[Int](socket.GetNbMembers) map (_.sum) onSuccess {
        case nb ⇒ env.metaHub ! socket.NbMembers(nb)
      }
    }

    effect(2 seconds, "fisherman: cleanup") {
      env.lobby.fisherman.cleanup
    }

    effect(10 seconds, "lobby: cleanup") {
      env.lobby.hookRepo.cleanupOld
    }

    unsafe(3 seconds, "usernameMemo: refresh") {
      env.metaHub.?[Iterable[String]](socket.GetUsernames) map (_.flatten) onSuccess {
        case xs ⇒ (env.user.usernameMemo putAll xs).unsafePerformIO
      }
    }

    if (current.mode != Mode.Dev) {

      env.ai.clientDiagnose

      effect(4.5 hours, "game: cleanup") {
        env.titivate.cleanupUnplayed flatMap { _ ⇒
          env.titivate.cleanupNext
        }
      }

      effect(1.13 hour, "game: finish by clock") {
        env.titivate.finishByClock
      }

      effect(2.3 hour, "game: finish abandoned") {
        env.titivate.finishAbandoned
      }
    }

    unsafe(10 seconds, "ai: diagnose") {
      env.ai.clientDiagnose
    }

    effect(5 minutes, "search: index finished games") {
      env.search.indexer.indexQueue
    }

    effect(2 hours, "search: optimize index") {
      env.search.indexer.optimize
    }

    unsafe(10 minutes, "firewall: refresh") {
      env.security.firewall.refresh
    }

    message(5 seconds) {
      env.tournament.organizer -> tournament.CreatedTournaments
    }

    message(3 seconds) {
      env.tournament.organizer -> tournament.StartedTournaments
    }

    message(3 seconds) {
      env.tournament.organizer -> tournament.StartPairings
    }

    def message(freq: FiniteDuration)(to: (ActorRef, Any)) {
      Akka.system.scheduler.schedule(freq, randomize(freq), to._1, to._2)
    }

    def effect(freq: FiniteDuration, name: String)(op: ⇒ IO[_]) {
      val f = randomize(freq)
      println("schedule effect %s every %s -> %s".format(name, freq, f))
      Akka.system.scheduler.schedule(f, f) {
        tryNamed(name, op.unsafePerformIO)
      }
    }

    def unsafe(freq: FiniteDuration, name: String)(op: ⇒ Unit) {
      Akka.system.scheduler.schedule(freq, randomize(freq)) {
        tryNamed(name, op)
      }
    }
  }

  private def tryNamed(name: String, op: ⇒ Unit) {
    try {
      op
    }
    catch {
      case e: Exception ⇒ println("[CRON ERROR] (" + name + ") " + e.getMessage)
    }
  }

  private def randomize(d: FiniteDuration, ratio: Float = 0.1f): FiniteDuration = {
    import scala.util.Random
    import scala.math.round
    import ornicar.scalalib.Random.approximatly

    approximatly(0.1f)(d.toMillis) millis
  }
}
