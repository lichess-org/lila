package lila.hub

import akka.actor._
import scala.concurrent.duration._

/**
 * Runs the work then waits cooldown
 * only runs once at a time per id.
 * Guarantees that work is ran as early as possible.
 * Also saves work and runs it after cooldown.
 */
final class EarlyMultiThrottler(
    executionTimeout: Option[FiniteDuration] = None,
    logger: lila.log.Logger) extends Actor {

  import EarlyMultiThrottler._

  var executions = Map.empty[String, Run]

  def receive: Receive = {

    case Work(id, run, cooldownOption, timeoutOption) if !executions.contains(id) =>
      implicit val system = context.system
      lila.common.Future.makeItLast(cooldownOption | 0.seconds) {
        timeoutOption.orElse(executionTimeout).fold(run()) { timeout =>
          run().withTimeout(
            duration = timeout,
            error = lila.common.LilaException(s"EarlyMultiThrottler timed out after $timeout"))
        }
      } andThenAnyway {
        self ! Done(id)
      }
      executions = executions + (id -> run)

    case _: Work => // already executing similar work

    case Done(id) =>
      executions = executions - id

    case x => logger.branch("EarlyMultiThrottler").warn(s"Unsupported message $x")
  }
}

object EarlyMultiThrottler {

  type Run = () => Funit

  case class Work(
    id: String,
    run: Run,
    cooldown: Option[FiniteDuration], // how long to wait after running, before next run
    timeout: Option[FiniteDuration]) // how long to wait before timing out

  case class Done(id: String)

  def work(
    id: String,
    run: => Funit,
    cooldown: Option[FiniteDuration] = None,
    timeout: Option[FiniteDuration] = None) =
    Work(id, () => run, cooldown, timeout)
}
