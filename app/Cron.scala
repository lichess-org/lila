package lila

import play.api._
import play.api.libs.concurrent.Akka
import akka.actor._
import akka.pattern.{ ask, pipe }
import akka.util.duration._
import akka.util.{ Duration, Timeout }
import akka.dispatch.{ Future }
import scalaz.effects._

import socket._
import lobby._
import game._

final class Cron(env: SystemEnv)(implicit app: Application) {

  implicit val timeout = Timeout(200 millis)
  implicit val executor = Akka.system.dispatcher

  spawnMessage("game_hub_keepalive", 2 second) {
    env.gameHubMaster -> KeepAlive
  }

  spawnMessage("hook_tick", 1 second) {
    env.lobbyHub -> WithHooks(env.hookMemo.putAll)
  }

  spawn("nb_players", 2 seconds) {
    Future.traverse(env.lobbyHub :: env.gameHubMaster :: Nil)(a ⇒
      (a ? GetNbMembers).mapTo[Int]
    ) map (xs ⇒ NbPlayers(xs.sum)) pipeTo env.lobbyHub pipeTo env.gameHubMaster
  }

  spawnIO("hook_cleanup_dead", 2 seconds) {
    env.lobbyFisherman.cleanup
  }

  spawnIO("hook_cleanup_old", 21 seconds) {
    env.hookRepo.cleanupOld
  }

  spawnMessage("online_username", 3 seconds) {
    env.lobbyHub -> WithUsernames(env.userRepo.updateOnlineUsernames)
  }

  spawnIO("game_cleanup_unplayed", 2 hours) {
    putStrLn("[cron] remove old unplayed games") flatMap { _ ⇒
      env.gameRepo.cleanupUnplayed
    }
  }

  spawnIO("game_auto_finish", 1 hour) {
    env.gameFinishCommand.apply
  }

  spawnIO("remote_ai_health", 10 seconds) {
    env.remoteAi.diagnose
  }

  def spawn(name: String, freq: Duration)(op: ⇒ Unit) = {
    Akka.system.scheduler.schedule(freq, freq)(op)
  }

  def spawnIO(name: String, freq: Duration)(op: IO[Unit]) = {
    Akka.system.scheduler.schedule(freq, freq) { op.unsafePerformIO }
  }

  def spawnMessage(name: String, freq: Duration)(to: (ActorRef, Any)) = {
    Akka.system.scheduler.schedule(freq, freq, to._1, to._2)
  }
}
