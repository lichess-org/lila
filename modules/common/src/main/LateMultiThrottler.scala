package lila.common

import akka.actor.*

/** Delays the work, only runs once at a time per id. Work is ran as late as possible.
  */
final class LateMultiThrottler(
    executionTimeout: Option[FiniteDuration] = None,
    logger: lila.log.Logger
)(using Executor)
    extends Actor:

  import LateMultiThrottler.*

  var executions = Set.empty[String]

  def receive: Receive =

    case Work(id, run, delayOption, timeoutOption) if !executions.contains(id) =>
      given Scheduler = context.system.scheduler
      lila.common.LilaFuture.delay(delayOption | 0.seconds):
        timeoutOption
          .orElse(executionTimeout)
          .fold(run()) { timeout =>
            run().withTimeout(timeout, "LateMultiThrottler")
          }
          .addEffectAnyway:
            self ! Done(id)
      executions = executions + id

    case _: Work => // already executing similar work
    case Done(id) =>
      executions = executions - id

    case x => logger.branch("LateMultiThrottler").warn(s"Unsupported message $x")

object LateMultiThrottler:

  def apply(
      executionTimeout: Option[FiniteDuration] = None,
      logger: lila.log.Logger
  )(using ec: Executor, system: ActorSystem) =
    system.actorOf(Props(new LateMultiThrottler(executionTimeout, logger)))

  case class Work(
      id: String,
      run: () => Funit,
      delay: Option[FiniteDuration],  // how long to wait before running
      timeout: Option[FiniteDuration] // how long to wait before timing out
  )

  private case class Done(id: String)

  def work[A](
      id: A,
      run: => Funit,
      delay: Option[FiniteDuration] = None,
      timeout: Option[FiniteDuration] = None
  )(using sr: StringRuntime[A]) =
    Work(sr(id), () => run, delay, timeout)
