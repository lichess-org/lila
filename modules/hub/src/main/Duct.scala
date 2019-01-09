package lila.hub

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import scala.collection.immutable.Queue
import scala.concurrent.duration._
import scala.concurrent.Promise

import lila.base.LilaException

/*
 * Sequential like an actor, but for async functions,
 * and using an STM backend instead of akka actor.
 */
trait Duct {

  import Duct._

  // implement async behaviour here
  protected val process: ReceiveAsync

  def !(msg: Any): Unit =
    if (stateRef.getAndUpdate(
      new UnaryOperator[State] {
        override def apply(state: State): State = Some(state.fold(Queue.empty[Any])(_ enqueue msg))
      }
    ).isEmpty) run(msg)

  def queueSize = stateRef.get().fold(0)(_.size + 1)

  /*
   * Idle: None
   * Busy: Some(Queue.empty)
   * Busy with backlog: Some(Queue.nonEmpty)
   */
  private[this] val stateRef: AtomicReference[State] = new AtomicReference(None)

  private[this] def run(msg: Any): Unit =
    process.applyOrElse(msg, Duct.fallback) onComplete postRun

  private[this] val postRun = (_: Any) =>
    stateRef.getAndUpdate(postRunUpdate) flatMap (_.headOption) foreach run
}

object Duct {

  type ReceiveAsync = PartialFunction[Any, Fu[Any]]

  private type State = Option[Queue[Any]]

  private val postRunUpdate = new UnaryOperator[State] {
    override def apply(state: State): State =
      state flatMap { q =>
        if (q.isEmpty) None else Some(q.tail)
      }
  }

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
        case LazyPromise(lf, promise) => promise.completeWith { lf(timeout)(system) }.future
      }
    }
  }
}
