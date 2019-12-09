package lila.common

import lila.log.Logger

import com.github.blemale.scaffeine.{ LoadingCache, Scaffeine }
import java.util.concurrent.atomic.AtomicReference
import scala.concurrent.duration.FiniteDuration

// Sequences async tasks
final class Sequencer(logger: Logger) {

  def run(task: () => Funit): Funit = queue.updateAndGet {
    _ flatMap { _ =>
      task().recover {
        case e: Exception => logger.error("Sequencer recover", e)
      }
    }
  }

  private val queue: AtomicReference[Funit] = new AtomicReference(funit)
}

// Distributes tasks to many sequencers
final class Sequencers(logger: Logger, expiration: FiniteDuration) {

  def run(key: String)(task: => Funit): Funit =
    sequencers.get(key).run(() => task)

  private val sequencers: LoadingCache[String, Sequencer] = Scaffeine()
    .expireAfterAccess(expiration)
    .build((key: String) => new Sequencer(logger branch key))
}
