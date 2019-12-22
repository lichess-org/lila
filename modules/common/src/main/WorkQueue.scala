package lila.common

import akka.stream.scaladsl._
import akka.stream.{ Materializer, OverflowStrategy, QueueOfferResult }
import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Future, Promise }
import scala.util.chaining._

/* Sequences async tasks, so that:
 * queue.run(() => task1); queue.run(() => task2)
 * runs task2 only after task1 completes, much like:
 * task1 flatMap { _ => task2 }
 *
 * If the buffer is full, the new task is dropped,
 * and `run` returns a failed future.
 */
final class WorkQueue(buffer: Int, name: String, parallelism: Int = 1)(
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
        lila.mon.workQueue.offerSuccess(name).increment()
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
        task() tap promise.completeWith recover {
          case e: Exception =>
            lila.mon.workQueue.taskFail(name).increment()
            lila.log(s"WorkQueue:$name").warn("task failed", e)
        }
    }
    .toMat(Sink.ignore)(Keep.left)
    .run
}

// Distributes tasks to many sequencers
final class WorkQueues(buffer: Int, expiration: FiniteDuration, name: String)(
    implicit ec: ExecutionContext,
    mat: Materializer
) {

  def apply(key: String)(task: => Funit): Funit =
    queues.get(key).run(() => task)

  private val queues: LoadingCache[String, WorkQueue] = Scaffeine()
    .expireAfterAccess(expiration)
    .build(key => new WorkQueue(buffer, s"$name:$key"))
}
