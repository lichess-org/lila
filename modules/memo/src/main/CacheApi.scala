package lila.memo

import akka.actor.ActorSystem
import com.github.benmanes.caffeine
import com.github.blemale.{ scaffeine => s }
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext

final class CacheApi(implicit ec: ExecutionContext, system: ActorSystem) {

  private type Builder = s.Scaffeine[Any, Any]

  val scaffeine: Builder = s.Scaffeine().scheduler(caffeine.cache.Scheduler.systemScheduler)

  def asyncLoading[K, V](
      name: String
  )(build: Builder => s.AsyncLoadingCache[K, V]): s.AsyncLoadingCache[K, V] = {
    val cache = build(scaffeine)
    monitor(name, cache)
    cache
  }

  def monitor(name: String, cache: s.AsyncCache[_, _]): Unit =
    monitor(name, cache.underlying.synchronous)

  def monitor(name: String, cache: s.Cache[_, _]): Unit =
    monitor(name, cache.underlying)

  def monitor(name: String, cache: caffeine.cache.Cache[_, _]): Unit =
    CacheApi.startMonitor(name, cache)
}

object CacheApi {

  implicit def beafedAsync[K, V](cache: s.AsyncCache[K, V])     = new BeafedAsync[K, V](cache)
  implicit def beafedAsyncUnit[V](cache: s.AsyncCache[Unit, V]) = new BeafedAsyncUnit[V](cache)
  implicit def beafedAsyncLoadingUnit[V](cache: s.AsyncLoadingCache[Unit, V]) =
    new BeafedAsyncLoadingUnit[V](cache)

  private[memo] def startMonitor(
      name: String,
      cache: caffeine.cache.Cache[_, _]
  )(implicit ec: ExecutionContext, system: ActorSystem): Unit =
    system.scheduler.scheduleWithFixedDelay(1 minute, 1 minute) { () =>
      lila.mon.caffeineStats(cache, name)
    }
}

final class BeafedAsync[K, V](val cache: s.AsyncCache[K, V]) extends AnyVal {

  def invalidate(key: K): Unit = cache.underlying.synchronous invalidate key
}

final class BeafedAsyncUnit[V](val cache: s.AsyncCache[Unit, V]) extends AnyVal {

  def invalidateUnit(): Unit = cache.underlying.synchronous.invalidate({})
}

final class BeafedAsyncLoadingUnit[V](val cache: s.AsyncLoadingCache[Unit, V]) extends AnyVal {

  def getUnit: Fu[V] = cache.get({})
}
