package lila.memo

import akka.actor.ActorSystem
import com.github.blemale.scaffeine.{ AsyncLoadingCache, Scaffeine }
import scala.concurrent.duration._

final class AsyncCache2[K, V] private (cache: AsyncLoadingCache[K, V], f: K => Fu[V]) {

  def get(k: K): Fu[V] = cache get k

  def refresh(k: K): Unit = cache.put(k, f(k))
}

object AsyncCache2 {

  def apply[K, V](
    name: String,
    f: K => Fu[V],
    maxCapacity: Int = 1024,
    expireAfter: ExpireAfter,
    resultTimeout: FiniteDuration = 5 seconds)(implicit system: ActorSystem) = {
    val safeF = (k: K) => f(k).withTimeout(
      duration = resultTimeout,
      error = lila.common.LilaException(s"AsyncCache $name timed out after $resultTimeout"))
    val b1 = Scaffeine().maximumSize(maxCapacity)
    val b2 = expireAfter match {
      case ExpireAfterAccess(duration) => b1 expireAfterAccess duration
      case ExpireAfterWrite(duration)  => b1 expireAfterWrite duration
    }
    new AsyncCache2[K, V](
      cache = b2.buildAsyncFuture(safeF),
      safeF)
  }

  sealed trait ExpireAfter
  case class ExpireAfterAccess(duration: FiniteDuration) extends ExpireAfter
  case class ExpireAfterWrite(duration: FiniteDuration) extends ExpireAfter
}

final class AsyncCache2Single[V] private (cache: AsyncLoadingCache[Unit, V], f: Unit => Fu[V]) {

  def get: Fu[V] = cache.get(())

  def refresh: Unit = cache.put((), f(()))
}

object AsyncCache2Single {

  def apply[V](
    name: String,
    f: => Fu[V],
    timeToLive: FiniteDuration,
    resultTimeout: FiniteDuration = 5 seconds)(implicit system: ActorSystem) = {
    val safeF = (_: Unit) => f.withTimeout(
      duration = resultTimeout,
      error = lila.common.LilaException(s"AsyncCache $name single timed out after $resultTimeout"))
    new AsyncCache2Single[V](
      cache = Scaffeine()
        .expireAfterWrite(timeToLive)
        .maximumSize(1)
        .buildAsyncFuture(safeF),
      safeF)
  }
}
