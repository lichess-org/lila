package lila.common

import akka.actor._
import akka.pattern.{ ask, pipe }
import scala.concurrent.duration._

final class Scheduler(system: ActorSystem, enabled: Boolean) {

  def message(freq: FiniteDuration)(to: (ActorRef, Any)) {
    enabled ! system.scheduler.schedule(freq, randomize(freq), to._1, to._2)
  }

  def effect(freq: FiniteDuration, name: String)(op: ⇒ Unit) {
    enabled ! future(freq, name)(fuccess(op))
  }

  def future(freq: FiniteDuration, name: String)(op: ⇒ Funit) {
    enabled ! {
      val f = randomize(freq)
      name.nonEmpty ! loginfo("[cron] schedule %s every %s".format(name, freq))
      system.scheduler.schedule(f, f) {
        op onFailure {
          case e: Throwable ⇒ println("[CRON ERROR] (" + name + ") " + e.getMessage)
        }
      }
    }
  }

  def once(freq: FiniteDuration)(op: ⇒ Unit) {
    enabled ! system.scheduler.scheduleOnce(freq)(op)
  }

  private def randomize(d: FiniteDuration, ratio: Float = 0.1f): FiniteDuration = {
    import scala.util.Random
    import scala.math.round
    import ornicar.scalalib.Random.approximatly

    approximatly(0.1f)(d.toMillis) millis
  }
}
