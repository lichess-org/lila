package lila.memo

import akka.actor.ActorSystem
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.github.benmanes.caffeine.cache.{ Cache => CaffeineCache }
import com.github.blemale.scaffeine.{ AsyncLoadingCache, Cache, Scaffeine }
import scala.concurrent.duration._

final class AsyncCache[K, V](cache: AsyncLoadingCache[K, V], f: K => Fu[V]) {

  def get(k: K): Fu[V] = cache get k

  def refresh(k: K): Unit = cache.put(k, f(k))

  def put(k: K, v: V): Unit = cache.put(k, fuccess(v))
}

final class AsyncCacheClearable[K, V](
    cache: Cache[K, Fu[V]],
    f: K => Fu[V],
    logger: lila.log.Logger
) {

  def get(k: K): Fu[V] = cache.get(k, (k: K) => {
    f(k).addFailureEffect { err =>
      logger.warn(s"$k $err")
      cache invalidate k
    }
  })

  def update(k: K, f: V => V): Unit =
    cache.getIfPresent(k) foreach { fu =>
      cache.put(k, fu map f)
    }

  def invalidate(k: K): Unit = cache invalidate k

  def invalidateAll: Unit = cache.invalidateAll
}

final class AsyncCacheSingle[V](cache: AsyncLoadingCache[Unit, V], f: Unit => Fu[V]) {

  def get: Fu[V] = cache.get(())

  def refresh: Unit = cache.put((), f(()))
}

object AsyncCache {

  final class Builder(implicit system: ActorSystem) {

    def multi[K, V](
      name: String,
      f: K => Fu[V],
      maxCapacity: Int = 32768,
      expireAfter: AsyncCache.type => ExpireAfter,
      resultTimeout: FiniteDuration = 5 seconds
    ) = {
      val safeF = (k: K) => f(k).withTimeout(
        resultTimeout,
        lila.base.LilaException(s"AsyncCache.multi $name key=$k timed out after $resultTimeout")
      )
      val cache: AsyncLoadingCache[K, V] = makeExpire(
        Scaffeine().maximumSize(maxCapacity),
        expireAfter
      ).recordStats.buildAsyncFuture(safeF)
      monitor(name, cache.underlying.synchronous)
      new AsyncCache[K, V](cache, safeF)
    }

    def clearable[K, V](
      name: String,
      f: K => Fu[V],
      maxCapacity: Int = 32768,
      expireAfter: AsyncCache.type => ExpireAfter,
      resultTimeout: FiniteDuration = 5 seconds
    ) = {
      val fullName = s"AsyncCache.clearable $name"
      val safeF = (k: K) => f(k).withTimeout(
        resultTimeout,
        lila.base.LilaException(s"$fullName key=$k timed out after $resultTimeout")
      )
      val cache: Cache[K, Fu[V]] = makeExpire(
        Scaffeine().maximumSize(maxCapacity),
        expireAfter
      ).recordStats.build[K, Fu[V]]
      monitor(name, cache.underlying)
      new AsyncCacheClearable[K, V](cache, safeF, logger = logger branch fullName)
    }

    def single[V](
      name: String,
      f: => Fu[V],
      expireAfter: AsyncCache.type => ExpireAfter,
      resultTimeout: FiniteDuration = 5 seconds
    ) = {
      val safeF = (_: Unit) => f.withTimeout(
        resultTimeout,
        lila.base.LilaException(s"AsyncCache.single $name single timed out after $resultTimeout")
      )
      val cache: AsyncLoadingCache[Unit, V] = makeExpire(
        Scaffeine().maximumSize(1),
        expireAfter
      ).recordStats.buildAsyncFuture(safeF)
      monitor(name, cache.underlying.synchronous)
      new AsyncCacheSingle[V](cache, safeF)
    }
  }

  private[memo] def monitor(name: String, cache: CaffeineCache[_, _])(implicit system: ActorSystem): Unit =
    system.scheduler.schedule(1 minute, 1 minute) {
      lila.mon.caffeineStats(cache, name)
    }

  sealed trait ExpireAfter
  case class ExpireAfterAccess(duration: FiniteDuration) extends ExpireAfter
  case class ExpireAfterWrite(duration: FiniteDuration) extends ExpireAfter

  private def makeExpire[K, V](
    builder: Scaffeine[K, V],
    expireAfter: AsyncCache.type => ExpireAfter
  ): Scaffeine[K, V] = expireAfter(AsyncCache) match {
    case ExpireAfterAccess(duration) => builder expireAfterAccess duration
    case ExpireAfterWrite(duration) => builder expireAfterWrite duration
  }
}
