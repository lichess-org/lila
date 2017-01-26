package lila.memo

import akka.actor.ActorSystem
import com.github.blemale.scaffeine.{ AsyncLoadingCache, Cache, Scaffeine }
import scala.concurrent.duration._

final class AsyncCache2[K, V](cache: AsyncLoadingCache[K, V], f: K => Fu[V]) {

  def get(k: K): Fu[V] = cache get k

  def refresh(k: K): Unit = cache.put(k, f(k))
}

final class AsyncCache2Clearable[K, V](cache: Cache[K, Fu[V]], f: K => Fu[V]) {

  def get(k: K): Fu[V] = cache.get(k, f)

  def invalidate(k: K): Unit = cache invalidate k

  def invalidateAll: Unit = cache.invalidateAll
}

final class AsyncCache2Single[V](cache: AsyncLoadingCache[Unit, V], f: Unit => Fu[V]) {

  def get: Fu[V] = cache.get(())

  def refresh: Unit = cache.put((), f(()))
}

object AsyncCache2 {

  final class Builder(implicit system: ActorSystem) {

    def multi[K, V](
      name: String,
      f: K => Fu[V],
      maxCapacity: Int = 32768,
      expireAfter: AsyncCache2.type => ExpireAfter,
      resultTimeout: FiniteDuration = 5 seconds) = {
      val safeF = (k: K) => f(k).withTimeout(
        duration = resultTimeout,
        error = lila.common.LilaException(s"AsyncCache $name timed out after $resultTimeout"))
      new AsyncCache2[K, V](
        cache = makeExpire(
          Scaffeine().maximumSize(maxCapacity),
          expireAfter
        ).buildAsyncFuture(safeF),
        safeF)
    }

    def clearable[K, V](
      name: String,
      f: K => Fu[V],
      maxCapacity: Int = 32768,
      expireAfter: AsyncCache2.type => ExpireAfter,
      resultTimeout: FiniteDuration = 5 seconds) = {
      val safeF = (k: K) => f(k).withTimeout(
        duration = resultTimeout,
        error = lila.common.LilaException(s"AsyncCache $name timed out after $resultTimeout"))
      new AsyncCache2Clearable[K, V](
        cache = makeExpire(
          Scaffeine().maximumSize(maxCapacity),
          expireAfter
        ).build[K, Fu[V]],
        safeF)
    }

    def single[V](
      name: String,
      f: => Fu[V],
      expireAfter: AsyncCache2.type => ExpireAfter,
      resultTimeout: FiniteDuration = 5 seconds) = {
      val safeF = (_: Unit) => f.withTimeout(
        duration = resultTimeout,
        error = lila.common.LilaException(s"AsyncCache $name single timed out after $resultTimeout"))
      new AsyncCache2Single[V](
        cache = makeExpire(
          Scaffeine().maximumSize(1),
          expireAfter
        ).buildAsyncFuture(safeF),
        safeF)
    }
  }

  sealed trait ExpireAfter
  case class ExpireAfterAccess(duration: FiniteDuration) extends ExpireAfter
  case class ExpireAfterWrite(duration: FiniteDuration) extends ExpireAfter

  private def makeExpire[K, V](
    builder: Scaffeine[K, V],
    expireAfter: AsyncCache2.type => ExpireAfter): Scaffeine[K, V] = expireAfter(AsyncCache2) match {
    case ExpireAfterAccess(duration) => builder expireAfterAccess duration
    case ExpireAfterWrite(duration)  => builder expireAfterWrite duration
  }
}
