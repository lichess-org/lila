package lila.memo

import com.github.blemale.scaffeine.Cache
import ornicar.scalalib.Zero
import scala.annotation.nowarn
import scala.concurrent.duration.FiniteDuration

final class ExpireSetMemo(ttl: FiniteDuration) {

  private val cache: Cache[String, Boolean] = CacheApi.scaffeineNoScheduler
    .expireAfterWrite(ttl)
    .build[String, Boolean]()

  @nowarn def get(key: String): Boolean = cache.underlying.getIfPresent(key) != null

  def intersect(keys: Iterable[String]): Set[String] =
    keys.nonEmpty ?? {
      val res = cache getAllPresent keys
      keys filter res.contains toSet
    }

  def put(key: String) = cache.put(key, true)

  def putAll(keys: Iterable[String]) = cache putAll keys.view.map(_ -> true).to(Map)

  def remove(key: String) = cache invalidate key

  def removeAll(keys: Iterable[String]) = cache invalidateAll keys

  def keys: Iterable[String] = cache.asMap().keys

  def keySet: Set[String] = keys.toSet

  def count = cache.estimatedSize().toInt
}

final class HashCodeExpireSetMemo[A](ttl: FiniteDuration) {

  private val cache: Cache[Int, Boolean] = CacheApi.scaffeineNoScheduler
    .expireAfterWrite(ttl)
    .build[Int, Boolean]()

  @nowarn def get(key: A): Boolean = cache.underlying.getIfPresent(key.hashCode) != null

  def put(key: A) = cache.put(key.hashCode, true)

  def remove(key: A) = cache invalidate key.hashCode

  // NOT thread-safe
  def once[B](key: A)(action: => B)(implicit default: Zero[B]) =
    if (!get(key)) {
      put(key)
      action
    } else default.zero
}
