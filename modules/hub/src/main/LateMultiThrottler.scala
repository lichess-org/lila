package lila.hub

import akka.actor._
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

/** Delays the work, only runs once at a time per id. Work is ran as late as possible.
  */
final class LateMultiThrottler(
    executionTimeout: Option[FiniteDuration] = None,
    logger: lila.log.Logger
)(implicit ec: ExecutionContext)
    extends Actor {

  import LateMultiThrottler._

  var executions = Set.empty[String]

  def receive: Receive = {

    case Work(id, run, delayOption, timeoutOption) if !executions.contains(id) =>
      implicit val system = context.system
      lila.common.Future.delay(delayOption | 0.seconds) {
        timeoutOption.orElse(executionTimeout).fold(run()) { timeout =>
          run().withTimeout(
            duration = timeout,
            error = lila.base.LilaException(s"LateMultiThrottler timed out after $timeout")
          )
        } addEffectAnyway {
          self ! Done(id)
        }
      }
      executions = executions + id

    case _: Work => // already executing similar work
    case Done(id) =>
      executions = executions - id

    case x => logger.branch("LateMultiThrottler").warn(s"Unsupported message $x")
  }
}

object LateMultiThrottler {

  def apply(
      executionTimeout: Option[FiniteDuration] = None,
      logger: lila.log.Logger
  )(implicit ec: ExecutionContext, system: ActorSystem) =
    system.actorOf(Props(new LateMultiThrottler(executionTimeout, logger)))

  case class Work(
      id: String,
      run: () => Funit,
      delay: Option[FiniteDuration],  // how long to wait before running
      timeout: Option[FiniteDuration] // how long to wait before timing out
  )

  case class Done(id: String)

  def work(
      id: String,
      run: => Funit,
      delay: Option[FiniteDuration] = None,
      timeout: Option[FiniteDuration] = None
  ) =
    Work(id, () => run, delay, timeout)
}
