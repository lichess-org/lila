package lila.common

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._

final class Scheduler(system: ActorSystem) {

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

  private def randomize(d: FiniteDuration, ratio: Float = 0.1f): FiniteDuration = {
    import scala.util.Random
    import scala.math.round
    import ornicar.scalalib.Random.approximatly

    approximatly(0.1f)(d.toMillis) millis
  }
}
