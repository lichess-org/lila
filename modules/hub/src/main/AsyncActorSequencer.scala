package lila.hub

import com.github.blemale.scaffeine.LoadingCache

import lila.common.config.Max

final class AsyncActorSequencer(maxSize: Max, timeout: FiniteDuration, name: String, logging: Boolean = true)(
    using
    Scheduler,
    Executor
):

  import AsyncActorSequencer.*

  def apply[A <: Matchable](fu: => Fu[A]): Fu[A] = run(() => fu)

  def run[A <: Matchable](task: Task[A]): Fu[A] = asyncActor.ask[A](TaskWithPromise(task, _))

  private[this] val asyncActor = BoundedAsyncActor(maxSize, name, logging) {
    case TaskWithPromise(task, promise) =>
      promise.completeWith {
        task().withTimeout(timeout, s"AsyncActorSequencer $name")
      }.future
  }

// Distributes tasks to many sequencers
final class AsyncActorSequencers[K](
    maxSize: Max,
    expiration: FiniteDuration,
    timeout: FiniteDuration,
    name: String,
    logging: Boolean = true
)(using Scheduler, Executor):

  def apply[A <: Matchable](key: K)(task: => Fu[A]): Fu[A] =
    sequencers.get(key).run(() => task)

  private val sequencers: LoadingCache[K, AsyncActorSequencer] =
    lila.common.LilaCache.scaffeine
      .expireAfterAccess(expiration)
      .build(key => AsyncActorSequencer(maxSize, timeout, s"$name:$key", logging))

object AsyncActorSequencer:

  private type Task[A <: Matchable] = () => Fu[A]
  private case class TaskWithPromise[A <: Matchable](task: Task[A], promise: Promise[A])
