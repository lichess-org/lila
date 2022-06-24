package lila.common

import scala.concurrent.duration._

/* Schedules an async function to be run periodically
 * Prevents concurrent execution of the function
 * Guarantees next execution even if the function fails or never completes
 */
object LilaScheduler {

  def apply(
      every: config.type => config.Every,
      timeout: config.type => config.AtMost,
      initialDelay: config.type => config.Delay
  )(f: => Funit)(implicit ec: scala.concurrent.ExecutionContext, system: akka.actor.ActorSystem): Unit = {

    val run = () => f

    def runAndScheduleNext(): Unit =
      run()
        .withTimeout(timeout(config).value)
        .addEffectAnyway {
          system.scheduler.scheduleOnce(every(config).value) { runAndScheduleNext() }.unit
        }
        .unit

    system.scheduler
      .scheduleOnce(initialDelay(config).value) {
        runAndScheduleNext()
      }
      .unit
  }
}
