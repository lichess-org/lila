package lila.hub

import com.github.blemale.scaffeine.LoadingCache
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Promise }

final class DuctSequencer(timeout: FiniteDuration, name: String)(implicit
    system: akka.actor.ActorSystem,
    ec: ExecutionContext
) {

  import DuctSequencer._

  def apply[A](fu: => Fu[A]): Fu[A] = run(() => fu)

  def run[A](task: Task[A]): Fu[A] = duct.ask[A](TaskWithPromise(task, _))

  private[this] val duct = new Duct {
    val process: Duct.ReceiveAsync = {
      case TaskWithPromise(task, promise) =>
        promise.completeWith {
          task().withTimeout(timeout)
        }.future
    }
  }
}

// Distributes tasks to many sequencers
final class DuctSequencers(expiration: FiniteDuration, timeout: FiniteDuration, name: String)(implicit
    system: akka.actor.ActorSystem,
    ec: ExecutionContext,
    mode: play.api.Mode
) {

  def apply[A](key: String)(task: => Fu[A]): Fu[A] =
    queues.get(key).run(() => task)

  private val queues: LoadingCache[String, DuctSequencer] =
    lila.common.LilaCache
      .scaffeine(mode)
      .expireAfterAccess(expiration)
      .build(key => new DuctSequencer(timeout, s"$name:$key"))
}

object DuctSequencer {

  type Task[A] = () => Fu[A]
  private case class TaskWithPromise[A](task: Task[A], promise: Promise[A])
}
