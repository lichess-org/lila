package lila.hub

import com.github.blemale.scaffeine.LoadingCache
import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ ExecutionContext, Promise }

import lila.base.LilaTimeout

final class AsyncActorSequencer(maxSize: Int, timeout: FiniteDuration, name: String, logging: Boolean = true)(
    implicit
    scheduler: akka.actor.Scheduler,
    ec: ExecutionContext
):

  import AsyncActorSequencer.*

  def apply[A <: Matchable](fu: => Fu[A]): Fu[A] = run(() => fu)

  def run[A <: Matchable](task: Task[A]): Fu[A] = asyncActor.ask[A](TaskWithPromise(task, _))

  private[this] val asyncActor =
    new BoundedAsyncActor(
      maxSize,
      name,
      logging
    )(
      { case TaskWithPromise(task, promise) =>
        promise.completeWith {
          task()
            .withTimeout(timeout)
            .transform(
              identity,
              {
                case LilaTimeout(msg) =>
                  val fullMsg = s"$name AsyncActorSequencer $msg"
                  if (logging) lila.log("asyncActor").warn(fullMsg)
                  LilaTimeout(fullMsg)
                case e => e
              }
            )
        }.future
      }
    )

// Distributes tasks to many sequencers
final class AsyncActorSequencers[K <: String](
    maxSize: Int,
    expiration: FiniteDuration,
    timeout: FiniteDuration,
    name: String,
    logging: Boolean = true
)(using
    scheduler: akka.actor.Scheduler,
    ec: ExecutionContext
):

  def apply[A <: Matchable](key: K)(task: => Fu[A]): Fu[A] =
    sequencers.get(key).run(() => task)

  private val sequencers: LoadingCache[K, AsyncActorSequencer] =
    lila.common.LilaCache.scaffeine
      .expireAfterAccess(expiration)
      .build(key => new AsyncActorSequencer(maxSize, timeout, s"$name:$key", logging))

object AsyncActorSequencer:

  private type Task[A <: Matchable] = () => Fu[A]
  private case class TaskWithPromise[A <: Matchable](task: Task[A], promise: Promise[A])
