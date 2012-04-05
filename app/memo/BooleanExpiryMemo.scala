package lila
package memo

import scalaz.effects._
import scala.collection.JavaConversions._

abstract class BooleanExpiryMemo(timeout: Int) {

  protected val cache = Builder.expiry[String, Boolean](timeout)

  def get(key: String): Boolean = Option {
    cache getIfPresent key
  } getOrElse false

  def put(key: String): IO[Unit] = io {
    cache.put(key, true)
  }

  def remove(key: String): IO[Unit] = io {
    cache invalidate key
  }

  def keys: Iterable[String] = cache.asMap.keys

  def count = cache.size
}
