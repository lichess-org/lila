package lila.common

import lila.log.Logger

import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.FiniteDuration

// Sequences async tasks, so that:
// queue.run(() => task1); queue.run(() => task2)
// runs task2 only after task1 completes, much like:
// task1 flatMap { _ => task2 }
final class WorkQueue(logger: Logger) {

  def run(task: () => Funit): Funit = queue.updateAndGet {
    _ flatMap { _ =>
      task().recover {
        case e: Exception => logger.error("WorkQueue recover", e)
      }
    }
  }

  private val queue: AtomicReference[Funit] = new AtomicReference(funit)
}

// Distributes tasks to many sequencers
final class WorkQueues(logger: Logger, expiration: FiniteDuration) {

  def run(key: String)(task: => Funit): Funit =
    queues.get(key).run(() => task)

  private val queues: LoadingCache[String, WorkQueue] = Scaffeine()
    .expireAfterAccess(expiration)
    .build((key: String) => new WorkQueue(logger branch key))
}
