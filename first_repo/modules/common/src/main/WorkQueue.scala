package lila.common

import akka.stream.scaladsl._
import akka.stream.{ Materializer, OverflowStrategy, QueueOfferResult }
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.chaining._
import java.util.concurrent.TimeoutException

/* Sequences async tasks, so that:
 * queue.run(() => task1); queue.run(() => task2)
 * runs task2 only after task1 completes, much like:
 * task1 flatMap { _ => task2 }
 *
 * If the buffer is full, the new task is dropped,
 * and `run` returns a failed future.
 *
 * This is known to work poorly with parallelism=1
 * because the queue is used by multiple threads
 */
final class WorkQueue(buffer: Int, timeout: FiniteDuration, name: String, parallelism: Int)(implicit
    ec: ExecutionContext,
    mat: Materializer
) {

  type Task[A]                    = () => Fu[A]
  private type TaskWithPromise[A] = (Task[A], Promise[A])

  def apply[A](future: => Fu[A]): Fu[A] = run(() => future)

  def run[A](task: Task[A]): Fu[A] = {
    val promise = Promise[A]()
    queue.offer(task -> promise) flatMap {
      case QueueOfferResult.Enqueued =>
        promise.future
      case result =>
        lila.mon.workQueue.offerFail(name, result.toString).increment()
        Future failed new WorkQueue.EnqueueException(s"Can't enqueue in $name: $result")
    }
  }

  private val queue = Source
    .queue[TaskWithPromise[_]](buffer, OverflowStrategy.dropNew) // #TODO use akka 2.6.11 BoundedQueueSource
    .mapAsyncUnordered(parallelism) { case (task, promise) =>
      task()
        .withTimeout(timeout, new TimeoutException)(ec, mat.system)
        .tap(promise.completeWith)
        .recover {
          case e: TimeoutException =>
            lila.mon.workQueue.timeout(name).increment()
            lila.log(s"WorkQueue:$name").warn(s"task timed out after $timeout", e)
          case e: Exception =>
            lila.log(s"WorkQueue:$name").info("task failed", e)
        }
    }
    .toMat(Sink.ignore)(Keep.left)
    .run()
}

object WorkQueue {

  final class EnqueueException(msg: String) extends Exception(msg)
}
