package lila.memo

import scala.concurrent.duration._

import spray.caching.{ LruCache, Cache }

final class MixedCache[K, V] private (
    cache: com.google.common.cache.LoadingCache[K, V],
    default: K => V) {

  def get(k: K): V = try {
    cache get k
  }
  catch {
    case _: java.util.concurrent.ExecutionException => default(k)
  }

  def invalidate(k: K) {
    cache invalidate k
  }
}

object MixedCache {

  def apply[K, V](
    f: K => Fu[V],
    timeToLive: Duration = Duration.Inf,
    awaitTime: FiniteDuration = 5.millis,
    default: K => V): MixedCache[K, V] = {
    val asyncCache = AsyncCache(f, maxCapacity = 10000, timeToLive = 1 minute)
    val syncCache = Builder.cache[K, V](
      timeToLive,
      (k: K) => asyncCache(k) await makeTimeout(awaitTime))
    new MixedCache(syncCache, default)
  }

  def single[V](
    f: => Fu[V],
    timeToLive: Duration = Duration.Inf,
    awaitTime: FiniteDuration = 10.millis,
    default: V): MixedCache[Boolean, V] = {
    val asyncCache = AsyncCache.single(f, timeToLive = 1 minute)
    val syncCache = Builder.cache[Boolean, V](
      timeToLive,
      (_: Boolean) => asyncCache(true) await makeTimeout(awaitTime))
    new MixedCache(syncCache, _ => default)
  }
}
