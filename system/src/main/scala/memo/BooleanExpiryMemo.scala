package lila.system
package memo

import scalaz.effects._
import collection.JavaConversions._

abstract class BooleanExpiryMemo(timeout: Int) {

  protected val cache = Builder.expiry[String, Boolean](timeout)

  def get(key: String): Boolean = Option {
    cache getIfPresent key
  } getOrElse false

  def put(key: String): IO[Unit] = io {
    cache.put(key, true)
  }

  def keys: Iterable[String] = cache.asMap.keys

  def count = cache.size
}
