package lila.memo

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration.Duration

final class ExpireSetMemo(ttl: Duration) {

  private val cache: Cache[String, Boolean] = Scaffeine()
    .expireAfterWrite(ttl)
    .build[String, Boolean]

  @inline private def isNotNull[A](a: A) = a != null

  def get(key: String): Boolean = isNotNull(cache.underlying getIfPresent key)

  def intersect(keys: Iterable[String]): Set[String] = keys.nonEmpty ?? {
    val res = cache getAllPresent keys
    keys filter res.contains toSet
  }

  def put(key: String) = cache.put(key, true)

  def putAll(keys: Iterable[String]) = cache putAll keys.view.map(_ -> true).to(Map)

  def remove(key: String) = cache invalidate key

  def removeAll(keys: Iterable[String]) = cache invalidateAll keys

  def keys: Iterable[String] = cache.asMap.keys

  def keySet: Set[String] = keys.toSet

  def count = cache.estimatedSize.toInt
}
