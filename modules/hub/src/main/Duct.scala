package lila.hub

import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.Promise

import java.util.concurrent.atomic.AtomicReference
import scala.compat.java8.FunctionConverters.asJavaUnaryOperator

import lila.base.LilaException

/*
 * Sequential like an actor, but for async functions.
 */
trait Duct {

  // implement async behaviour here
  protected val process: Duct.ReceiveAsync

  def !(msg: Any): Unit =
    if (stateRef.getAndUpdate(asJavaUnaryOperator { q =>
      Some(q.fold(Queue.empty[Any])(_ enqueue msg))
    }) isEmpty) run(msg)

  def queueSize = stateRef.get().??(_.size)

  /*
   * Idle: None
   * Busy: Some(Queue.empty)
   * Busy with backlog: Some(Queue.nonEmpty)
   */
  private[this] val stateRef: AtomicReference[Option[Queue[Any]]] = new AtomicReference(None)

  private[this] def run(msg: Any): Unit =
    process.applyOrElse(msg, Duct.fallback) onComplete postRun

  private[this] val postRun = (_: Any) =>
    stateRef.getAndUpdate(asJavaUnaryOperator {
      _ flatMap { q =>
        if (q.isEmpty) None else Some(q.tail)
      }
    }) flatMap (_.headOption) foreach run
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

      def apply(timeout: Option[FiniteDuration])(implicit system: akka.actor.ActorSystem) =
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
    def lazyFu(timeout: FiniteDuration)(implicit system: akka.actor.ActorSystem) = new Duct {
      val process: Duct.ReceiveAsync = { case lf: LazyFu[_] => lf(timeout.some) }
    }

    case class LazyPromise[A](f: LazyFu[A], promise: Promise[A])

    def lazyPromise(timeout: Option[FiniteDuration])(implicit system: akka.actor.ActorSystem) = new Duct {
      val process: Duct.ReceiveAsync = {
        case lf: LazyFu[_] => lf(timeout)
        case LazyPromise(lf, promise) => promise.completeWith { lf(timeout)(system) }.future
      }
    }
  }
}
