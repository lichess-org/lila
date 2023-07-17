package lila.hub

import java.util.concurrent.atomic.AtomicReference
import java.util.function.UnaryOperator
import scala.collection.immutable.Queue
import lila.common.config.Max

/*
 * Sequential like an actor, but for async functions,
 * and using an atomic backend instead of akka actor.
 */
final class BoundedAsyncActor(maxSize: Max, name: String, logging: Boolean = true)(
    process: AsyncActor.ReceiveAsync
)(using Executor):

  import BoundedAsyncActor.*

  def !(msg: Matchable): Boolean =
    stateRef.getAndUpdate { state =>
      Some {
        state.fold(emptyQueue) { q =>
          if q.size >= maxSize.value then q
          else q enqueue msg
        }
      }
    } match
      case None => // previous state was idle, we can run immediately
        run(msg)
        true
      case Some(q) =>
        val success = q.size < maxSize.value
        if !success then
          lila.mon.asyncActor.overflow(name).increment()
          if logging then lila.log("asyncActor").warn(s"[$name] queue is full ($maxSize)")
        else if logging && q.size >= monitorQueueSize then lila.mon.asyncActor.queueSize(name).record(q.size)
        success

  def ask[A](makeMsg: Promise[A] => Matchable): Fu[A] =
    val promise = Promise[A]()
    val success = this ! makeMsg(promise)
    if !success then promise failure new EnqueueException(s"The $name asyncActor queue is full ($maxSize)")
    promise.future

  def queueSize = stateRef.get().fold(0)(_.size + 1)

  private val monitorQueueSize = maxSize.value / 4

  /*
   * Idle: None
   * Busy: Some(Queue.empty)
   * Busy with backlog: Some(Queue.nonEmpty)
   */
  private[this] val stateRef: AtomicReference[State] = new AtomicReference(None)

  private[this] def run(msg: Matchable): Unit =
    process.applyOrElse(msg, fallback) onComplete postRun

  private[this] val postRun = (_: Matchable) =>
    stateRef.getAndUpdate(postRunUpdate) flatMap (_.headOption) foreach run

  private[this] lazy val fallback = (msg: Any) =>
    lila.log("asyncActor").warn(s"[$name] unhandled msg: $msg")
    funit

object BoundedAsyncActor:

  final class EnqueueException(msg: String) extends Exception(msg)

  private case class SizedQueue(queue: Queue[Matchable], size: Int):
    def enqueue(a: Matchable) = SizedQueue(queue enqueue a, size + 1)
    def isEmpty               = size == 0
    def tailOption            = !isEmpty option SizedQueue(queue.tail, size - 1)
    def headOption            = queue.headOption
  private val emptyQueue = SizedQueue(Queue.empty, 0)

  private type State = Option[SizedQueue]

  private val postRunUpdate = new UnaryOperator[State]:
    override def apply(state: State): State = state.flatMap(_.tailOption)
