package lila.common

import akka.stream.scaladsl._
import akka.stream.{ Materializer, OverflowStrategy, QueueOfferResult }
import com.github.blemale.scaffeine.LoadingCache
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
 */
final class WorkQueue(buffer: Int, timeout: FiniteDuration, name: String, parallelism: Int = 1)(
    implicit ec: ExecutionContext,
    mat: Materializer
) {

  type Task[A]                    = () => Fu[A]
  private type TaskWithPromise[A] = (Task[A], Promise[A])

  def apply[A](future: => Fu[A]): Fu[A] = run(() => future)

  def run[A](task: Task[A]): Fu[A] = {
    val promise = Promise[A]
    queue.offer(task -> promise) flatMap {
      case QueueOfferResult.Enqueued =>
        promise.future
      case result =>
        lila.mon.workQueue.offerFail(name, result.toString).increment()
        Future failed new Exception(s"Can't enqueue in $name: $result")
    }
  }

  private val queue = Source
    .queue[TaskWithPromise[_]](buffer, OverflowStrategy.dropNew)
    .mapAsyncUnordered(parallelism) {
      case (task, promise) =>
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
    .run
}

// Distributes tasks to many sequencers
final class WorkQueues(buffer: Int, expiration: FiniteDuration, timeout: FiniteDuration, name: String)(
    implicit ec: ExecutionContext,
    mat: Materializer,
    mode: play.api.Mode
) {

  def apply(key: String)(task: => Funit): Funit =
    queues.get(key).run(() => task)

  private val queues: LoadingCache[String, WorkQueue] =
    LilaCache
      .scaffeine(mode)
      .expireAfterAccess(expiration)
      .build(key => new WorkQueue(buffer, timeout, s"$name:$key"))
}
