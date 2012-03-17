package lila.system
package memo

import com.google.common.base.Function
import com.google.common.cache._
import java.util.concurrent.TimeUnit
import scalaz.Memo

object Builder extends scalaz.Memos {

  /**
   * A caching wrapper for a function (K => V),
   * backed by a Cache from Google Collections.
   */
  def cache[K, V](ttl: Int): Memo[K, V] = memo[K, V] { (f: (K ⇒ V)) ⇒
    val map = CacheBuilder.newBuilder()
      .expireAfterAccess(ttl, TimeUnit.SECONDS)
      .asInstanceOf[CacheBuilder[K, V]]
      .build[K, V](f)
      .asMap()
    (k: K) ⇒ map.get(k)
  }

  implicit def functionToGoogleFunction[T, R](f: T ⇒ R): Function[T, R] =
    new Function[T, R] {
      def apply(p1: T) = f(p1)
    }

  implicit def functionToGoogleCacheLoader[T, R](f: T ⇒ R): CacheLoader[T, R] =
    new CacheLoader[T, R] {
      def load(p1: T) = f(p1)
    }
}
