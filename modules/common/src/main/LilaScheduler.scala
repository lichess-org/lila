package lila.common

import lila.core.config

/* Schedules an async function to be run periodically
 * Prevents concurrent execution of the function
 * Guarantees next execution even if the function fails or never completes
 */
object LilaScheduler:

  def apply(
      name: String,
      every: config.type => config.Every,
      timeout: config.type => config.AtMost,
      initialDelay: config.type => config.Delay
  )(f: => Funit)(using ec: Executor, scheduler: Scheduler): Unit =

    val run = () => f

    def runAndScheduleNext(): Unit =
      run()
        .withTimeout(timeout(config).value, s"LilaScheduler $name")
        .addEffectAnyway:
          scheduler.scheduleOnce(every(config).value) { runAndScheduleNext() }

    scheduler
      .scheduleOnce(initialDelay(config).value):
        runAndScheduleNext()
