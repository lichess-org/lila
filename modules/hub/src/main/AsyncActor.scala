package lila.hub

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import scala.collection.immutable.Queue

/*
 * Sequential like an actor, but for async functions,
 * and using an atomic backend instead of akka actor.
 */
abstract class AsyncActor(using Executor) extends lila.common.Tellable:

  import AsyncActor.*

  // implement async behaviour here
  protected val process: ReceiveAsync

  def !(msg: Matchable): Unit =
    if stateRef.getAndUpdate(state => Some(state.fold(Queue.empty[Matchable])(_ enqueue msg))).isEmpty then
      run(msg)

  def ask[A](makeMsg: Promise[A] => Matchable): Fu[A] =
    val promise = Promise[A]()
    this ! makeMsg(promise)
    promise.future

  /*
   * Idle: None
   * Busy: Some(Queue.empty)
   * Busy with backlog: Some(Queue.nonEmpty)
   */
  private[this] val stateRef: AtomicReference[State] = new AtomicReference(None)

  private[this] def run(msg: Matchable): Unit =
    process.applyOrElse(msg, AsyncActor.fallback) onComplete postRun

  private[this] val postRun = (_: Matchable) =>
    stateRef.getAndUpdate(postRunUpdate) flatMap (_.headOption) foreach run

object AsyncActor:

  type ReceiveAsync = PartialFunction[Matchable, Fu[Matchable]]

  private type State = Option[Queue[Matchable]]

  private val postRunUpdate = new UnaryOperator[State]:
    override def apply(state: State): State =
      state flatMap { q =>
        if q.isEmpty then None else Some(q.tail)
      }

  private val fallback = (msg: Matchable) =>
    lila.log("asyncActor").warn(s"unhandled msg: $msg")
    funit
