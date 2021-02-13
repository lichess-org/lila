package lila.memo

import com.github.blemale.scaffeine.Cache
import scala.concurrent.duration.FiniteDuration

// calls a function when a key expires
final class ExpireCallbackMemo(ttl: FiniteDuration, callback: String => Unit)(implicit mode: play.api.Mode) {

  private val cache: Cache[String, Boolean] = lila.memo.CacheApi
    .scaffeine(mode)
    .expireAfterWrite(ttl)
    .removalListener((key: String, _: Boolean, _) => callback(key))
    .build[String, Boolean]()

  @inline private def isNotNull[A](a: A) = a != null

  def get(key: String): Boolean = isNotNull(cache.underlying getIfPresent key)

  def put(key: String) = cache.put(key, true)

  def remove(key: String) = cache invalidate key

  def count = cache.estimatedSize().toInt

  def keySet: Set[String] = cache.asMap().keys.toSet
}
