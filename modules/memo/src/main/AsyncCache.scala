package lila.memo

import scala.concurrent.duration._

import spray.caching.{ LruCache, Cache }

final class AsyncCache[K, V] private (cache: Cache[V], f: K => Fu[V]) {

  def apply(k: K): Fu[V] = cache(k)(f(k))

  def get(k: K): Option[Fu[V]] = cache get k

  def remove(k: K): Funit = fuccess(cache remove k).void

  def clear: Funit = fuccess(cache.clear)
}

object AsyncCache {

  def apply[K, V](
    f: K => Fu[V],
    maxCapacity: Int = 500,
    initialCapacity: Int = 16,
    timeToLive: Duration = Duration.Inf,
    timeToIdle: Duration = Duration.Inf) = new AsyncCache(
    cache = LruCache(maxCapacity, initialCapacity, timeToLive, timeToIdle),
    f = f)

  def single[V](
    f: => Fu[V],
    timeToLive: Duration = Duration.Inf) = new AsyncCache[Boolean, V](
    cache = LruCache(timeToLive = timeToLive),
    f = _ => f)

  def mixed[K, V](
    f: K => Fu[V],
    timeToLive: Duration = Duration.Inf): com.google.common.cache.LoadingCache[K, V] = {
    val asyncCache = apply(f, maxCapacity = 10000, timeToLive = 1 minute)
    Builder.cache[K, V](timeToLive, (k: K) => asyncCache(k) await 1.second)
  }
}
