package lila

import play.api._
import play.api.libs.concurrent.Akka
import akka.actor.ActorRef
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import scalaz.effects._

final class Cron(env: SystemEnv) {

  implicit val current = env.app
  implicit val timeout = Timeout(500 millis)

  unsafe(5 seconds) {
    (env.siteHub :: env.lobbyHub :: env.gameHubMemo.hubs) foreach { actor =>
      actor ! socket.Broom
    }
  }

  message(2 seconds) {
    env.reporting -> report.Update(env)
  }

  message(1 second) {
    env.lobbyHub -> lobby.WithHooks(env.hookMemo.putAll)
  }

  message(2 seconds) {
    env.siteHub -> site.NbMembers
  }

  effect(2 seconds) {
    env.lobbyFisherman.cleanup
  }

  effect(10 seconds) {
    env.hookRepo.cleanupOld
  }

  message(3 seconds) {
    env.siteHub -> site.WithUsernames(env.userRepo.updateOnlineUsernames)
  }

  effect(4.1 hours) {
    env.gameRepo.cleanupUnplayed flatMap { _ ⇒
      env.gameCleanNextCommand.apply
    }
  }

  effect(1 hour) {
    env.gameFinishCommand.apply
  }

  effect(10 seconds) {
    env.remoteAi.diagnose
  }

  import RichDuration._

  def message(freq: Duration)(to: (ActorRef, Any)) {
    Akka.system.scheduler.schedule(freq, freq.randomize(), to._1, to._2)
  }

  def effect(freq: Duration)(op: IO[_]) {
    Akka.system.scheduler.schedule(freq, freq.randomize()) { op.unsafePerformIO }
  }

  def unsafe(freq: Duration)(op: => Unit) {
    Akka.system.scheduler.schedule(freq, freq.randomize()) { op }
  }
}
