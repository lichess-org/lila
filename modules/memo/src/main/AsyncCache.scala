package lila.memo

import akka.actor.ActorSystem
import com.github.blemale.scaffeine.{ AsyncLoadingCache, Cache, Scaffeine }
import scala.concurrent.duration._

final class AsyncCache[K, V](cache: AsyncLoadingCache[K, V], f: K => Fu[V]) {

  def get(k: K): Fu[V] = cache get k

  def refresh(k: K): Unit = cache.put(k, f(k))
}

final class AsyncCacheClearable[K, V](
    cache: Cache[K, Fu[V]],
    f: K => Fu[V],
    logger: lila.log.Logger) {

  def get(k: K): Fu[V] = cache.get(k, (k: K) => {
    f(k).addFailureEffect { err =>
      logger.warn(s"$k $err")
      cache invalidate k
    }
  })

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
      resultTimeout: FiniteDuration = 5 seconds) = {
      val safeF = (k: K) => f(k).withTimeout(
        resultTimeout,
        lila.common.LilaException(s"AsyncCache.multi $name key=$k timed out after $resultTimeout"))
      new AsyncCache[K, V](
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
      expireAfter: AsyncCache.type => ExpireAfter,
      resultTimeout: FiniteDuration = 5 seconds) = {
      val fullName = s"AsyncCache.clearable $name"
      val safeF = (k: K) => f(k).withTimeout(
        resultTimeout,
        lila.common.LilaException(s"$fullName key=$k timed out after $resultTimeout"))
      new AsyncCacheClearable[K, V](
        cache = makeExpire(
          Scaffeine().maximumSize(maxCapacity),
          expireAfter
        ).build[K, Fu[V]],
        safeF,
        logger = logger branch fullName)
    }

    def single[V](
      name: String,
      f: => Fu[V],
      expireAfter: AsyncCache.type => ExpireAfter,
      resultTimeout: FiniteDuration = 5 seconds) = {
      val safeF = (_: Unit) => f.withTimeout(
        resultTimeout,
        lila.common.LilaException(s"AsyncCache.single $name single timed out after $resultTimeout"))
      new AsyncCacheSingle[V](
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
    expireAfter: AsyncCache.type => ExpireAfter): Scaffeine[K, V] = expireAfter(AsyncCache) match {
    case ExpireAfterAccess(duration) => builder expireAfterAccess duration
    case ExpireAfterWrite(duration)  => builder expireAfterWrite duration
  }
}
