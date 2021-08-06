package lila.common

import akka.actor._
import scala.concurrent.duration._

/* Schedules an async function to be run periodically
 * Prevents concurrent execution of the function
 * Guarantees next execution even if the function fails or never completes
 */
object ResilientScheduler {

  def apply(
      every: Every,
      timeout: AtMost,
      initialDelay: FiniteDuration
  )(f: => Funit)(implicit ec: scala.concurrent.ExecutionContext, system: ActorSystem): Unit = {
    val run = () => f
    def runAndScheduleNext(): Unit =
      run()
        .withTimeout(timeout.value)
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
