package lila.hub

import akka.actor.ActorSystem
import java.util.concurrent.atomic._
import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.Promise

/**
 * Sequential like an actor, but for async functions.
 */
trait Duct {

  // implement async behaviour here
  protected val process: Duct.ReceiveAsync

  def !(msg: Any): Unit =
    if (state.getAndUpdate(_ enqueue msg) isEmpty) run(msg)

  def queueSize = state.get.size

  /*
   * Queue contains msg currently processing, as well as backlog.
   * Idle: queue.isEmpty
   * Busy: queue.size == 1
   * Busy with backlog: queue.size > 1
   */
  private[this] val state: AtomicReference[Queue[Any]] = new AtomicReference(Queue.empty)

  private[this] def run(msg: Any): Unit =
    process.applyOrElse(msg, Duct.fallback) onComplete postRun

  private[this] val postRun = (_: Any) =>
    state.updateAndGet(_.tail).headOption foreach run
}

object Duct {

  type ReceiveAsync = PartialFunction[Any, Fu[Any]]

  private val fallback = { msg: Any =>
    lila.log("Duct").warn(s"unhandled msg: $msg")
    funit
  }

  case class Timeout(duration: FiniteDuration) extends lila.base.LilaException {
    val message = s"FutureSequencer timed out after $duration"
  }

  /* Convenience functions to build upon Ducts */
  object extra {

    case class LazyFu[A](f: () => Fu[A]) {

      def apply(timeout: Option[FiniteDuration])(implicit system: ActorSystem) =
        timeout.foldLeft(f()) { (fu, dur) =>
          fu.withTimeout(
            duration = dur,
            error = Timeout(dur)
          )
        }
    }

    // MUST be a def. Ducts cannot be reused.
    def lazyFu = new Duct {
      val process: Duct.ReceiveAsync = { case LazyFu(f) => f() }
    }
    def lazyFu(timeout: FiniteDuration)(implicit system: ActorSystem) = new Duct {
      val process: Duct.ReceiveAsync = { case lf: LazyFu[_] => lf(timeout.some) }
    }

    case class LazyPromise[A](f: LazyFu[A], promise: Promise[A])

    def lazyPromise(timeout: Option[FiniteDuration])(implicit system: ActorSystem) = new Duct {
      val process: Duct.ReceiveAsync = {
        case lf: LazyFu[_] => lf(timeout)
        case LazyPromise(lf, promise) => promise.completeWith { lf(timeout)(system) }.future
      }
    }
  }
}
