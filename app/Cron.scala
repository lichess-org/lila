package lila

import play.api._
import play.api.libs.concurrent.Akka
import akka.actor.ActorRef
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import scalaz.effects.IO

import site.{ NbMembers, WithUsernames }
import lobby.WithHooks
import RichDuration._

final class Cron(env: SystemEnv)(implicit app: Application) {

  implicit val timeout = Timeout(200 millis)
  implicit val executor = Akka.system.dispatcher

  message(1 second) {
    env.lobbyHub -> WithHooks(env.hookMemo.putAll)
  }

  message(1 seconds) {
    env.siteHub -> NbMembers
  }

  io(2 seconds) {
    env.lobbyFisherman.cleanup
  }

  io(10 seconds) {
    env.hookRepo.cleanupOld
  }

  message(3 seconds) {
    env.siteHub -> WithUsernames(env.userRepo.updateOnlineUsernames)
  }

  io(2 hours) {
    env.gameRepo.cleanupUnplayed
  }

  io(1 hour) {
    env.gameFinishCommand.apply
  }

  io(10 seconds) {
    env.remoteAi.diagnose
  }

  def io(freq: Duration)(op: IO[Unit]) {
    Akka.system.scheduler.schedule(freq, freq.randomize()) { op.unsafePerformIO }
  }

  def message(freq: Duration)(to: (ActorRef, Any)) {
    Akka.system.scheduler.schedule(freq, freq.randomize(), to._1, to._2)
  }
}
