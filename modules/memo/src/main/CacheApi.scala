package lila.memo

import com.github.benmanes.caffeine
import com.github.blemale.scaffeine.*
import alleycats.Zero

final class CacheApi(using Executor, Scheduler)(using mode: play.api.Mode):

  import CacheApi.*

  // AsyncLoadingCache with monitoring
  def apply[K, V](initialCapacity: Int, name: String)(
      build: Builder => AsyncLoadingCache[K, V]
  ): AsyncLoadingCache[K, V] =
    val cache = build:
      scaffeine.recordStats().initialCapacity(actualCapacity(initialCapacity))
    monitor(name, cache)
    cache

  // AsyncLoadingCache for a single entry
  def unit[V](build: Builder => AsyncLoadingCache[Unit, V]): AsyncLoadingCache[Unit, V] =
    build(scaffeine.initialCapacity(1))

  // AsyncLoadingCache with monitoring and a synchronous getter
  def sync[K, V](
      name: String,
      initialCapacity: Int,
      compute: K => Fu[V],
      default: K => V,
      strategy: Syncache.Strategy,
      expireAfter: Syncache.ExpireAfter
  ): Syncache[K, V] =
    val actualCapacity =
      if mode.isProd then initialCapacity
      else math.sqrt(initialCapacity.toDouble).toInt.atLeast(1)
    val cache = new Syncache(name, actualCapacity, compute, default, strategy, expireAfter)
    monitor(name, cache.cache)
    cache

  def notLoading[K, V](initialCapacity: Int, name: String)(
      build: Builder => AsyncCache[K, V]
  ): AsyncCache[K, V] =
    val cache = build:
      scaffeine.recordStats().initialCapacity(actualCapacity(initialCapacity))
    monitor(name, cache)
    cache

  def notLoadingSync[K, V](initialCapacity: Int, name: String)(
      build: Builder => Cache[K, V]
  ): Cache[K, V] =
    val cache = build:
      scaffeine.recordStats().initialCapacity(actualCapacity(initialCapacity))
    monitor(name, cache)
    cache

  def monitor(name: String, cache: AsyncCache[?, ?]): Unit =
    monitor(name, cache.underlying.synchronous)

  def monitor(name: String, cache: Cache[?, ?]): Unit =
    monitor(name, cache.underlying)

  def monitor(name: String, cache: caffeine.cache.Cache[?, ?]): Unit =
    startMonitor(name, cache)

  def actualCapacity(c: Int) =
    if mode.isProd then c else math.sqrt(c.toDouble).toInt.atLeast(1)

object CacheApi:

  export scalalib.cache.{ scaffeine, scaffeineNoScheduler }

  private[memo] type Builder = Scaffeine[Any, Any]

  extension [K, V](cache: AsyncCache[K, V])

    def invalidate(key: K): Unit = cache.underlying.synchronous.invalidate(key)
    def invalidateAll(): Unit = cache.underlying.synchronous.invalidateAll()

    def update(key: K, f: V => V): Unit =
      cache
        .getIfPresent(key)
        .foreach: v =>
          cache.put(key, v.dmap(f))

  extension [V](cache: AsyncCache[Unit, V])
    def invalidateUnit(): Unit = cache.underlying.synchronous.invalidate {}

  extension [V](cache: AsyncLoadingCache[Unit, V]) def getUnit: Fu[V] = cache.get {}

  extension [K, V](builder: Scaffeine[K, V])

    def buildAsyncTimeout[K1 <: K, V1 <: V](loaderTimeout: FiniteDuration = 10.seconds)(
        loader: K1 => Future[V1]
    )(using Scheduler, Executor): AsyncLoadingCache[K1, V1] =
      builder.buildAsyncFuture: k =>
        loader(k).withTimeout(loaderTimeout, s"buildAsyncFuture($k)")

    def buildAsyncTimeoutZero[K1 <: K, V1 <: V](loaderTimeout: FiniteDuration = 10.seconds)(
        loader: K1 => Future[V1]
    )(using zero: Zero[V1])(using Scheduler, Executor): AsyncLoadingCache[K1, V1] =
      builder.buildAsyncFuture: k =>
        loader(k).withTimeoutDefault(loaderTimeout, zero.zero)

  private[memo] def startMonitor(
      name: String,
      cache: caffeine.cache.Cache[?, ?]
  )(using ec: Executor, scheduler: Scheduler): Unit =
    scheduler.scheduleWithFixedDelay(1.minute, 1.minute): () =>
      lila.mon.caffeineStats(cache, name)
