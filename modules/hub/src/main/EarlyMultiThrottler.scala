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
    logger: lila.log.Logger
) extends Actor {

  import EarlyMultiThrottler._

  var running = Set.empty[String]
  var planned = Map.empty[String, Work]

  def receive: Receive = {

    case work: Work if !running(work.id) =>
      execute(work) addEffectAnyway {
        self ! Done(work.id)
      }
      running = running + work.id

    case work: Work => // already executing similar work
      planned = planned + (work.id -> work)

    case Done(id) =>
      running = running - id
      planned get id foreach { work =>
        self ! work
        planned = planned - work.id
      }

    case x => logger.branch("EarlyMultiThrottler").warn(s"Unsupported message $x")
  }

  def execute(work: Work): Funit = {
    implicit val system = context.system
    lila.common.Future.makeItLast(work.cooldown) {
      work.timeout.orElse(executionTimeout).fold(work.run()) { timeout =>
        work.run().withTimeout(
          duration = timeout,
          error = lila.base.LilaException(s"EarlyMultiThrottler timed out after $timeout")
        )
      }
    }
  }
}

object EarlyMultiThrottler {

  case class Work(
      id: String,
      run: () => Funit,
      cooldown: FiniteDuration, // how long to wait after running, before next run
      timeout: Option[FiniteDuration]
  ) // how long to wait before timing out

  def work(
    id: String,
    run: => Funit,
    cooldown: FiniteDuration,
    timeout: Option[FiniteDuration] = None
  ) =
    Work(id, () => run, cooldown, timeout)

  private case class Done(id: String)
}
