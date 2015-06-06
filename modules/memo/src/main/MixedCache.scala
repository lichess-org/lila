package lila.memo

import scala.concurrent.duration._

import com.google.common.cache.{ LoadingCache => SyncCache }
import spray.caching.{ LruCache, Cache }

final class MixedCache[K, V] private (
    cache: SyncCache[K, V],
    default: K => V,
    val invalidate: K => Funit) {

  def get(k: K): V = try {
    cache get k
  }
  catch {
    case _: java.util.concurrent.ExecutionException => default(k)
    case e: com.google.common.util.concurrent.UncheckedExecutionException =>
      logerr(e.getMessage)
      default(k)
  }
}

object MixedCache {

  private def invalidate[K](async: AsyncCache[K, _], sync: SyncCache[K, _])(k: K): Funit =
    async.remove(k) >>- sync.invalidate(k)

  def apply[K, V](
    f: K => Fu[V],
    timeToLive: Duration = Duration.Inf,
    awaitTime: FiniteDuration = 5.milliseconds,
    default: K => V): MixedCache[K, V] = {
    val async = AsyncCache(f, maxCapacity = 10000, timeToLive = 1 minute)
    val sync = Builder.cache[K, V](
      timeToLive,
      (k: K) => async(k) await makeTimeout(awaitTime))
    new MixedCache(sync, default, invalidate(async, sync) _)
  }

  def single[V](
    f: => Fu[V],
    timeToLive: Duration = Duration.Inf,
    awaitTime: FiniteDuration = 5.milliseconds,
    default: V): MixedCache[Boolean, V] = {
    val async = AsyncCache.single(f, timeToLive = 1 minute)
    val sync = Builder.cache[Boolean, V](
      timeToLive,
      (_: Boolean) => async(true) await makeTimeout(awaitTime))
    new MixedCache(sync, _ => default, invalidate(async, sync) _)
  }
}
