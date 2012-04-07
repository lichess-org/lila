package lila

import play.api._
import play.api.libs.concurrent.Akka
import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.duration._
import akka.util.Duration
import akka.util.Timeout
import scalaz.effects._

final class Cron(env: SystemEnv)(implicit app: Application) {

  implicit val timeout = Timeout(200 millis)

  spawn("hook_tick") {
    env.lobbyHub ? lobby.GetHooks onSuccess {
      case lobby.Hooks(ownerIds) ⇒ (env.hookMemo putAll ownerIds).unsafePerformIO
    }
  }

  spawn("heart_beat") {
    val future = for {
      lobbyCount ← env.lobbyHub ? lobby.GetCount mapTo manifest[Int]
    } yield lobbyCount
    future map { lobby.NbPlayers(_) } pipeTo env.lobbyHub
  }

  spawnIO("hook_cleanup_dead") {
    env.lobbyFisherman.cleanup
  }

  spawnIO("hook_cleanup_old") {
    env.hookRepo.cleanupOld
  }

  spawn("online_username") {
    val future = for {
      lobbyUsernames ← env.lobbyHub ? lobby.GetUsernames
    } yield lobbyUsernames
    future onSuccess {
      case lobby.Usernames(usernames) ⇒
      (env.userRepo updateOnlineUsernames usernames).unsafePerformIO
    }
  }

  spawnIO("game_cleanup_unplayed") {
    putStrLn("[cron] remove old unplayed games") flatMap { _ ⇒
      env.gameRepo.cleanupUnplayed
    }
  }

  spawnIO("game_auto_finish") {
    env.gameFinishCommand.apply()
  }

  spawnIO("remote_ai_health") {
    for {
      health ← env.remoteAi.health
      _ ← health.fold(
        env.remoteAiHealth.fold(io(), putStrLn("remote AI is up")),
        putStrLn("remote AI is down")
      )
      _ ← io { env.remoteAiHealth = health }
    } yield ()
  }

  def spawn(name: String)(op: ⇒ Unit) = {
    val freq = frequency(name)
    Akka.system.scheduler.schedule(freq, freq)(op)
  }

  def spawnIO(name: String)(op: ⇒ IO[Unit]) = {
    val freq = frequency(name)
    Akka.system.scheduler.schedule(freq, freq) { op.unsafePerformIO }
  }

  def frequency(name: String) = configDuration("cron.frequency.%s" format name)

  def configDuration(key: String) = env.getMilliseconds(key) millis
}
