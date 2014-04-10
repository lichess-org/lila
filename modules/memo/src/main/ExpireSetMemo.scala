package lila.memo

import scala.collection.JavaConversions._
import scala.concurrent.duration.Duration

final class ExpireSetMemo(ttl: Duration) {

  protected val cache = Builder.expiry[String, Boolean](ttl)

  def get(key: String): Boolean = Option(cache getIfPresent key) getOrElse false

  def put(key: String) { cache.put(key, true) }

  def putAll(keys: Iterable[String]) {
    keys.toList.distinct foreach { cache.put(_, true) }
  }

  def remove(key: String) { cache invalidate key }

  def keys: Iterable[String] = cache.asMap.keys

  def keySet: Set[String] = keys.toSet

  def count = keys.toList.size
}
