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

  spawn("heart_beat") { env ⇒
    implicit val timeout = Timeout(100 millis)
    io {
      val future = for {
        lobbyCount ← env.lobbyHub ? lobby.Count mapTo manifest[Int]
      } yield lobbyCount
      future map { lobby.NbPlayers(_) } pipeTo env.lobbyHub
    }
  }

  spawn("hook_cleanup_dead") { env ⇒
    env.lobbyFisherman.cleanup
  }

  spawn("hook_cleanup_old") { env ⇒
    env.hookRepo.cleanupOld
  }

  spawn("online_username") { env ⇒
    env.userRepo updateOnlineUsernames env.usernameMemo.keys
  }

  spawn("game_cleanup_unplayed") { env ⇒
    putStrLn("[cron] remove old unplayed games") flatMap { _ ⇒
      env.gameRepo.cleanupUnplayed
    }
  }

  spawn("game_auto_finish") { _.gameFinishCommand.apply() }

  spawn("remote_ai_health") { env ⇒
    for {
      health ← env.remoteAi.health
      _ ← health.fold(
        env.remoteAiHealth.fold(io(), putStrLn("remote AI is up")),
        putStrLn("remote AI is down")
      )
      _ ← io { env.remoteAiHealth = health }
    } yield ()
  }

  private def spawn(name: String)(f: SystemEnv ⇒ IO[Unit]) = {
    val freq = configDuration("cron.frequency.%s" format name)
    val actor = Akka.system.actorOf(Props(new Actor {
      def receive = {
        case Tick ⇒ f(env).unsafePerformIO
      }
    }), name = name)
    Akka.system.scheduler.schedule(freq, freq, actor, Tick)
  }

  private def configDuration(key: String) = env.getMilliseconds(key) millis
}
