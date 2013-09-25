package lila.common

import scala.concurrent.duration._

import akka.actor._
import akka.pattern.{ ask, pipe }
import ornicar.scalalib.Random.{approximatly, nextString}

final class Scheduler(system: ActorSystem, enabled: Boolean, debug: Boolean) {

  def message(freq: FiniteDuration)(to: ⇒ (ActorRef, Any)) {
    enabled ! system.scheduler.schedule(freq, randomize(freq), to._1, to._2)
  }

  def messageToSelection(freq: FiniteDuration)(to: ⇒ (ActorSelection, Any)) {
    enabled ! system.scheduler.schedule(freq, randomize(freq)) {
      to._1 ! to._2
    }
  }

  def effect(freq: FiniteDuration, name: String)(op: ⇒ Unit) {
    enabled ! future(freq, name)(fuccess(op))
  }

  def future(freq: FiniteDuration, name: String)(op: ⇒ Funit) {
    enabled ! {
      val f = randomize(freq)
      val doDebug = debug && freq > 5.seconds
      info("schedule %s every %s".format(name, freq))
      system.scheduler.schedule(f, f) {
        val tagged = "(%s) %s".format(nextString(3), name)
        doDebug ! info(tagged)
        val start = nowMillis
        op effectFold (
          e ⇒ err("(%s) %s".format(tagged, e.getMessage)),
          _ ⇒ doDebug ! info(tagged + " - %d ms".format(nowMillis - start))
        )
      }
    }
  }

  def once(delay: FiniteDuration)(op: ⇒ Unit) {
    enabled ! system.scheduler.scheduleOnce(delay)(op)
  }

  private def info(msg: String) {
    println("[cron] " + msg)
  }

  private def err(msg: String) {
    println("[cron error] " + msg)
  }

  private def randomize(d: FiniteDuration, ratio: Float = 0.05f): FiniteDuration =
    approximatly(ratio)(d.toMillis) millis
}
