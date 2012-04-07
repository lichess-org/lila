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

  configDuration("lobby.hook_pool.tick.frequency") |> { freq ⇒
    Akka.system.scheduler.schedule(freq, freq, env.lobbyHookPool, Tick)
  }

  spawn("heart_beat") {
    implicit val timeout = Timeout(100 millis)
    io {
      val future = for {
        lobbyCount ← env.lobbyHub ? lobby.Count mapTo manifest[Int]
      } yield lobbyCount
      future map { lobby.NbPlayers(_) } pipeTo env.lobbyHub
    }
  }

  spawn("hook_cleanup_dead") {
    env.lobbyFisherman.cleanup
  }

  spawn("hook_cleanup_old") {
    env.hookRepo.cleanupOld
  }

  spawn("online_username") {
    env.userRepo updateOnlineUsernames env.usernameMemo.keys
  }

  spawn("game_cleanup_unplayed") {
    putStrLn("[cron] remove old unplayed games") flatMap { _ ⇒
      env.gameRepo.cleanupUnplayed
    }
  }

  spawn("game_auto_finish") {
    env.gameFinishCommand.apply()
  }

  spawn("remote_ai_health") {
    for {
      health ← env.remoteAi.health
      _ ← health.fold(
        env.remoteAiHealth.fold(io(), putStrLn("remote AI is up")),
        putStrLn("remote AI is down")
      )
      _ ← io { env.remoteAiHealth = health }
    } yield ()
  }

  private def spawn(name: String)(op: ⇒ IO[Unit]) = {
    val freq = configDuration("cron.frequency.%s" format name)
    Akka.system.scheduler.schedule(freq, freq) {
      op.unsafePerformIO
    }
  }

  private def configDuration(key: String) = env.getMilliseconds(key) millis
}
