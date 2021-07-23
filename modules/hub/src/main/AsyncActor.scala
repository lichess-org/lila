package lila.hub

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import scala.collection.immutable.Queue
import scala.concurrent.Promise

/*
 * Sequential like an actor, but for async functions,
 * and using an atomic backend instead of akka actor.
 */
abstract class AsyncActor(implicit ec: scala.concurrent.ExecutionContext) extends lila.common.Tellable {

  import AsyncActor._

  // implement async behaviour here
  protected val process: ReceiveAsync

  def !(msg: Any): Unit =
    if (stateRef.getAndUpdate(state => Some(state.fold(Queue.empty[Any])(_ enqueue msg))).isEmpty) run(msg)

  def ask[A](makeMsg: Promise[A] => Any): Fu[A] = {
    val promise = Promise[A]()
    this ! makeMsg(promise)
    promise.future
  }

  /*
   * Idle: None
   * Busy: Some(Queue.empty)
   * Busy with backlog: Some(Queue.nonEmpty)
   */
  private[this] val stateRef: AtomicReference[State] = new AtomicReference(None)

  private[this] def run(msg: Any): Unit =
    process.applyOrElse(msg, AsyncActor.fallback) onComplete postRun

  private[this] val postRun = (_: Any) =>
    stateRef.getAndUpdate(postRunUpdate) flatMap (_.headOption) foreach run
}

object AsyncActor {

  type ReceiveAsync = PartialFunction[Any, Fu[Any]]

  private type State = Option[Queue[Any]]

  private val postRunUpdate = new UnaryOperator[State] {
    override def apply(state: State): State =
      state flatMap { q =>
        if (q.isEmpty) None else Some(q.tail)
      }
  }

  private val fallback = { msg: Any =>
    lila.log("asyncActor").warn(s"unhandled msg: $msg")
    funit
  }
}
