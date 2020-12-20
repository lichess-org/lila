package lila.memo

import com.github.blemale.scaffeine.Cache
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
