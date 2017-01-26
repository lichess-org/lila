package lila.memo

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration.Duration

final class ExpireSetMemo(ttl: Duration) {

  private val cache: Cache[String, Boolean] = Scaffeine()
    .expireAfterWrite(ttl)
    .build[String, Boolean]

  def get(key: String): Boolean = cache getIfPresent key getOrElse false

  def put(key: String) = cache.put(key, true)

  def putAll(keys: Iterable[String]) {
    keys.toList.distinct foreach { cache.put(_, true) }
  }

  def remove(key: String) = cache invalidate key

  def keys: Iterable[String] = cache.asMap.keys

  def keySet: Set[String] = keys.toSet

  def count = cache.asMap.size
}
