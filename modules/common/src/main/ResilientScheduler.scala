package lila.common

import akka.actor._
import scala.concurrent.duration._

object ResilientScheduler {

  private case object Tick
  private case object Done

  def apply(
      every: Every,
      atMost: AtMost,
      initialDelay: FiniteDuration
  )(f: => Funit)(implicit ec: scala.concurrent.ExecutionContext, system: ActorSystem): Unit = {
    val run = () => f
    def runAndScheduleNext(): Unit =
      run()
        .withTimeout(atMost.value)
        .addEffectAnyway {
          system.scheduler.scheduleOnce(every.value) { runAndScheduleNext() }.unit
        }
        .unit
    system.scheduler
      .scheduleOnce(initialDelay) {
        runAndScheduleNext()
      }
      .unit
  }
}
