package lila.app

import lila.socket.actorApi._
import lila.hub.actorApi.Ask

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._
import play.api.libs.concurrent._
import play.api.Mode

object Cron {

  def start(system: ActorSystem) {

    loginfo("[boot] cron (" + Env.api.mode + ")")

    implicit val timeout = makeTimeout(500 millis)
    val socketHub = Env.hub.socket.hub

    // message(5 seconds) {
    //   socketHub -> Broom
    // }

    // message(1 seconds) {
    //   Env.monitor.reporting -> monitor.Update(Env)
    // }

    // message(1 second) {
    //   Env.lobby.hub -> lobby.WithHooks(Env.lobby.hookMemo.putAll)
    // }

    effect(2 seconds, "meta hub: refresh") {
      socketHub ? Ask(GetNbMembers) mapTo manifest[Seq[Int]] map { nbs ⇒
        NbMembers(nbs.sum)
      } pipeTo socketHub
    }

    // effect(2 seconds, "fisherman: cleanup") {
    //   Env.lobby.fisherman.cleanup
    // }

    // effect(10 seconds, "lobby: cleanup") {
    //   Env.lobby.hookRepo.cleanupOld
    // }

    // unsafe(3 seconds, "usernameMemo: refresh") {
    //   Env.metaHub.?[Iterable[String]](socket.GetUsernames) map (_.flatten) onSuccess {
    //     case xs ⇒ (Env.user.usernameMemo putAll xs).unsafePerformIO
    //   }
    // }

    // if (current.mode != Mode.Dev) {

    //   Env.ai.clientDiagnose

    //   effect(4.5 hours, "game: cleanup") {
    //     Env.titivate.cleanupUnplayed flatMap { _ ⇒
    //       Env.titivate.cleanupNext
    //     }
    //   }

    //   effect(1.13 hour, "game: finish by clock") {
    //     Env.titivate.finishByClock
    //   }

    //   effect(2.3 hour, "game: finish abandoned") {
    //     Env.titivate.finishAbandoned
    //   }
    // }

    // unsafe(10 seconds, "ai: diagnose") {
    //   Env.ai.clientDiagnose
    // }

    // effect(5 minutes, "search: index finished games") {
    //   Env.search.indexer.indexQueue
    // }

    // effect(2 hours, "search: optimize index") {
    //   Env.search.indexer.optimize
    // }

    // unsafe(10 minutes, "firewall: refresh") {
    //   Env.security.firewall.refresh
    // }

    // message(5 seconds) {
    //   Env.tournament.organizer -> tournament.CreatedTournaments
    // }

    // message(3 seconds) {
    //   Env.tournament.organizer -> tournament.StartedTournaments
    // }

    // message(3 seconds) {
    //   Env.tournament.organizer -> tournament.StartPairings
    // }

    def message(freq: FiniteDuration)(to: (ActorRef, Any)) {
      system.scheduler.schedule(freq, randomize(freq), to._1, to._2)
    }

    def effect(freq: FiniteDuration, name: String)(op: ⇒ Unit) {
      future(freq, name)(fuccess(op))
    }

    def future(freq: FiniteDuration, name: String)(op: ⇒ Funit) {
      val f = randomize(freq)
      loginfo("[cron] schedule %s every %s".format(name, freq))
      system.scheduler.schedule(f, f) {
        op onFailure {
          case e: Throwable ⇒ println("[CRON ERROR] (" + name + ") " + e.getMessage)
        }
      }
    }
  }

  private def randomize(d: FiniteDuration, ratio: Float = 0.1f): FiniteDuration = {
    import scala.util.Random
    import scala.math.round
    import ornicar.scalalib.Random.approximatly

    approximatly(0.1f)(d.toMillis) millis
  }
}
