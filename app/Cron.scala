package lila

import play.api._
import play.api.libs.concurrent.Akka
import akka.actor._
import akka.pattern.{ pipe }
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import scalaz.effects._

import lobby._

final class Cron(env: SystemEnv)(implicit app: Application) {

  implicit val timeout = Timeout(200 millis)

  spawn("hook_tick") {
    env.lobbyHub ! WithHooks(env.hookMemo.putAll)
  }

  spawnMessage("heart_beat", env.lobbyHub, NbPlayers)

  spawnIO("hook_cleanup_dead") {
    env.lobbyFisherman.cleanup
  }

  spawnIO("hook_cleanup_old") {
    env.hookRepo.cleanupOld
  }

  spawn("online_username") {
    env.lobbyHub ! WithUsernames(env.userRepo.updateOnlineUsernames)
  }

  spawnIO("game_cleanup_unplayed") {
    putStrLn("[cron] remove old unplayed games") flatMap { _ ⇒
      env.gameRepo.cleanupUnplayed
    }
  }

  spawnIO("game_auto_finish") {
    env.gameFinishCommand.apply
  }

  spawnIO("remote_ai_health") {
    env.remoteAi.diagnose
  }

  def spawn(name: String)(op: ⇒ Unit) = {
    val freq = frequency(name)
    Akka.system.scheduler.schedule(freq, freq)(op)
  }

  def spawnIO(name: String)(op: IO[Unit]) = {
    val freq = frequency(name)
    Akka.system.scheduler.schedule(freq, freq) { op.unsafePerformIO }
  }

  def spawnMessage(name: String, actor: ActorRef, message: Any) = {
    val freq = frequency(name)
    Akka.system.scheduler.schedule(freq, freq, actor, message)
  }

  def frequency(name: String) = configDuration("cron.frequency.%s" format name)

  def configDuration(key: String) = env.getMilliseconds(key) millis
}
