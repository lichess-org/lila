package lila.hub

import akka.actor._
import scala.concurrent.duration._

final class MultiThrottler(
    executionTimeout: Option[FiniteDuration] = None,
    logger: lila.log.Logger) extends Actor {

  var executions = Map.empty[String, Unit]

  def receive: Receive = {

    case MultiThrottler.Work(id, run, delayOption, timeoutOption) if !executions.contains(id) =>
      implicit val system = context.system
      val fut = lila.common.Future.delay(delayOption | 0.seconds) {
        timeoutOption.orElse(executionTimeout).fold(run()) { timeout =>
          run().withTimeout(
            duration = timeout,
            error = lila.common.LilaException(s"Throttler timed out after $timeout")
          )(context.system)
        } andThenAnyway {
          self ! MultiThrottler.Done(id)
        }
      }
      executions = executions + (id -> fut)

    case _: MultiThrottler.Work => // already executing similar work

    case MultiThrottler.Done(id) =>
      executions = executions - id

    case x => logger.branch("MultiThrottler").warn(s"Unsupported message $x")
  }
}

object MultiThrottler {

  case class Work(
    id: String,
    run: () => Funit,
    delay: Option[FiniteDuration],
    timeout: Option[FiniteDuration])

  case class Done(id: String)

  def work(
    id: String,
    run: => Funit,
    delay: Option[FiniteDuration] = None,
    timeout: Option[FiniteDuration] = None) =
    Work(id, () => run, delay, timeout)
}
