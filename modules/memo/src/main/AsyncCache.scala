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

  implicit private val system = lila.common.PlayApp.system

  def apply[K, V](
    name: String,
    f: K => Fu[V],
    maxCapacity: Int = 500,
    initialCapacity: Int = 16,
    timeToLive: Duration = Duration.Inf,
    timeToIdle: Duration = Duration.Inf,
    resultTimeout: FiniteDuration = 5 seconds) = new AsyncCache(
    cache = LruCache(maxCapacity, initialCapacity, timeToLive, timeToIdle),
    f = (key: K) => f(key).withTimeout(
      duration = resultTimeout,
      error = lila.common.LilaException(s"AsyncCache $name $key timed out after $resultTimeout"))
  )

  def single[V](
    f: => Fu[V],
    timeToLive: Duration = Duration.Inf) = new AsyncCache[Boolean, V](
    cache = LruCache(timeToLive = timeToLive),
    f = _ => f)
}
