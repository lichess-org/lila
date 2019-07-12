package lila.memo

import com.github.blemale.scaffeine.{ Cache, Scaffeine }
import scala.concurrent.duration.Duration

// calls a function when a key expires
final class ExpireCallbackMemo(ttl: Duration, callback: String => Unit) {

  private val cache: Cache[String, Boolean] = Scaffeine()
    .expireAfterWrite(ttl)
    .removalListener((key: String, value: Boolean, cause) => callback(key))
    .build[String, Boolean]

  def put(key: String) = cache.put(key, true)

  def remove(key: String) = cache invalidate key

  def count = cache.estimatedSize.toInt
}
