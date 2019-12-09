package lila.common

import lila.log.Logger

import akka.stream.scaladsl._
import akka.stream.{ Materializer, OverflowStrategy }
import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Promise

// Sequences async tasks, so that:
// queue.run(() => task1); queue.run(() => task2)
// runs task2 only after task1 completes, much like:
// task1 flatMap { _ => task2 }
final class WorkQueue(buffer: Int, logger: Logger)(implicit mat: Materializer) {

  type Task = () => Funit
  private type TaskWithPromise = (Task, Promise[Unit])

  def run(task: Task): Funit = {
    val promise = Promise[Unit]
    queue.offer(task -> promise)
    promise.future
  }

  private val queue = Source
    .queue[TaskWithPromise](buffer, OverflowStrategy.dropHead)
    .mapAsync(1) {
      case (task, promise) =>
        task()
          .map(promise.success)
          .recover {
            case e: Exception => logger.error("WorkQueue recover", e)
          }
    }
    .toMat(Sink.ignore)(Keep.left)
    .run
}

// Distributes tasks to many sequencers
final class WorkQueues(buffer: Int, logger: Logger, expiration: FiniteDuration)(implicit mat: Materializer) {

  def run(key: String)(task: => Funit): Funit =
    queues.get(key).run(() => task)

  private val queues: LoadingCache[String, WorkQueue] = Scaffeine()
    .expireAfterAccess(expiration)
    .build((key: String) => new WorkQueue(buffer, logger branch key))
}
