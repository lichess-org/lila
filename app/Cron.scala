package lila

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

object Cron {

  def start(env: SystemEnv) {

    implicit val current = env.app
    implicit val timeout = Timeout(500 millis)
    implicit val executor = Akka.system.dispatcher

    unsafe(5 seconds) {
      (env.siteHub :: env.lobbyHub :: env.gameHubMaster :: Nil) foreach { actor ⇒
        actor ! socket.Broom
      }
    }

    message(5 seconds) {
      env.reporting -> report.Update(env)
    }

    message(1 second) {
      env.lobbyHub -> lobby.WithHooks(env.hookMemo.putAll)
    }

    unsafe(2 seconds) {
      Future.traverse(hubs) { hub ⇒
        hub ? socket.GetNbMembers mapTo manifest[Int]
      } map (_.sum) onSuccess {
        case nb ⇒ hubs foreach { _ ! socket.NbMembers(nb) }
      }
    }

    effect(2 seconds) {
      env.lobbyFisherman.cleanup
    }

    effect(10 seconds) {
      env.hookRepo.cleanupOld
    }

    unsafe(3 seconds) {
      Future.traverse(hubs) { hub ⇒
        hub ? socket.GetUsernames mapTo manifest[Iterable[String]]
      } map (_.flatten) onSuccess {
        case xs ⇒ (for {
          _ ← env.usernameMemo putAll xs
          _ ← env.userRepo.updateOnlineUsernames(env.usernameMemo.keys.toSet)
        } yield Unit).unsafePerformIO
      }
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
    env.remoteAi.diagnose.unsafePerformIO

    import RichDuration._

    lazy val hubs: List[ActorRef] = 
      List(env.siteHub, env.lobbyHub, env.gameHubMaster)

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
