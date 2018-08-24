package lidraughts.hub

import scala.concurrent.duration._
import scala.concurrent.Promise

final class FutureSequencer(
    system: akka.actor.ActorSystem,
    executionTimeout: Option[FiniteDuration] = None,
    logger: lidraughts.log.Logger
) {

  import FutureSequencer._

  def apply[A](op: => Fu[A]): Fu[A] = {
    val promise = Promise[A]()
    duct ! Op(() => op, promise)
    promise.future
  }

  def queueSize = duct.queueSize

  private[this] val duct = new Duct {
    val process: Duct.ReceiveAsync = {
      case Op(f, promise) => promise.completeWith {
        executionTimeout.foldLeft(f()) { (fu, timeout) =>
          fu.withTimeout(
            duration = timeout,
            error = Timeout(timeout)
          )(system)
        }
      }.future
    }
  }
}

object FutureSequencer {

  private case class Op[A](f: () => Fu[A], promise: Promise[A])

  case class Timeout(duration: FiniteDuration) extends lidraughts.base.LidraughtsException {
    val message = s"FutureSequencer timed out after $duration"
  }
}
