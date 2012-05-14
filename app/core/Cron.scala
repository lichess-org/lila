package lila
package core

import play.api._
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

    unsafe(5 seconds) {
      (env.site.hub :: env.lobby.hub :: env.round.hubMaster :: Nil) foreach { actor ⇒
        actor ! socket.Broom
      }
    }

    message(5 seconds) {
      env.site.reporting -> report.Update(env)
    }

    message(1 second) {
      env.lobby.hub -> lobby.WithHooks(env.lobby.hookMemo.putAll)
    }

    unsafe(2 seconds) {
      Future.traverse(hubs) { hub ⇒
        hub ? socket.GetNbMembers mapTo manifest[Int]
      } map (_.sum) onSuccess {
        case nb ⇒ hubs foreach { _ ! socket.NbMembers(nb) }
      }
    }

    effect(2 seconds) {
      env.lobby.fisherman.cleanup
    }

    effect(10 seconds) {
      env.lobby.hookRepo.cleanupOld
    }

    unsafe(3 seconds) {
      Future.traverse(hubs) { hub ⇒
        hub ? socket.GetUsernames mapTo manifest[Iterable[String]]
      } map (_.flatten) onSuccess {
        case xs ⇒ (for {
          _ ← env.user.usernameMemo putAll xs
          _ ← env.user.userRepo.updateOnlineUsernames(env.user.usernameMemo.keys.toSet)
        } yield Unit).unsafePerformIO
      }
    }

    effect(4.1 hours) {
      env.game.gameRepo.cleanupUnplayed flatMap { _ ⇒
        env.gameCleanNextCommand.apply
      }
    }

    effect(1 hour) {
      env.gameFinishCommand.apply
    }

    effect(10 seconds) {
      env.ai.remoteAi.diagnose
    }
    env.ai.remoteAi.diagnose.unsafePerformIO

    lazy val hubs: List[ActorRef] = 
      List(env.site.hub, env.lobby.hub, env.round.hubMaster)

    def message(freq: Duration)(to: (ActorRef, Any)) {
      Akka.system.scheduler.schedule(freq, freq.randomize(), to._1, to._2)
    }

    def effect(freq: Duration)(op: IO[_]) {
      Akka.system.scheduler.schedule(freq, freq.randomize()) { op.unsafePerformIO }
    }

    def unsafe(freq: Duration)(op: ⇒ Unit) {
      Akka.system.scheduler.schedule(freq, freq.randomize()) { op }
    }
  }
}
