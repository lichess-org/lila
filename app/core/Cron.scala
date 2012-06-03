package lila
package core

import play.api.Mode
import play.api.libs.concurrent.Akka
import akka.actor.ActorRef
import akka.pattern.{ ask, pipe }
import akka.dispatch.{ Future, Promise }
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import play.api.libs.concurrent._
import play.api.Play.current
import scalaz.effects._

import implicits.RichDuration._

object Cron {

  def start(env: CoreEnv) {

    implicit val current = env.app
    implicit val timeout = Timeout(500 millis)
    implicit val executor = Akka.system.dispatcher

    println("Start cron mode " + current.mode)

    unsafe(5 seconds) {
      env.metaHub ! socket.Broom
    }

    message(1 seconds) {
      env.monitor.reporting -> monitor.Update(env)
    }

    message(1 second) {
      env.lobby.hub -> lobby.WithHooks(env.lobby.hookMemo.putAll)
    }

    unsafe(2 seconds) {
      env.metaHub.?[Int](socket.GetNbMembers) map (_.sum) onSuccess {
        case nb ⇒ env.metaHub ! socket.NbMembers(nb)
      }
    }

    effect(2 seconds, "fisherman cleanup") {
      env.lobby.fisherman.cleanup
    }

    effect(10 seconds, "lobby cleanup") {
      env.lobby.hookRepo.cleanupOld
    }

    unsafe(3 seconds) {
      env.metaHub.?[Iterable[String]](socket.GetUsernames) map (_.flatten) onSuccess {
        case xs ⇒ (env.user.usernameMemo putAll xs).unsafePerformIO
      }
    }

    if (current.mode != Mode.Dev) {

      effect(4.1 hours, "game cleanup") {
        env.titivate.cleanupUnplayed flatMap { _ ⇒
          env.titivate.cleanupNext
        }
      }

      effect(1 hour, "game finish") {
        env.titivate.finishByClock
      }

      effect(10 seconds, "ai diagnose") {
        env.ai.remoteAi.diagnose
      }
      env.ai.remoteAi.diagnose.unsafePerformIO
    }

    def message(freq: Duration)(to: (ActorRef, Any)) {
      Akka.system.scheduler.schedule(freq, freq.randomize(), to._1, to._2)
    }

    def effect(freq: Duration, name: String)(op: IO[_]) {
      val f = freq.randomize()
      //val f = freq
      println("schedule effect %s every %s -> %s".format(name, freq, f))
      Akka.system.scheduler.schedule(f, f) {
        op.unsafePerformIO
      }
    }

    def unsafe(freq: Duration)(op: ⇒ Unit) {
      Akka.system.scheduler.schedule(freq, freq.randomize()) { op }
    }
  }
}
