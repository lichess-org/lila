package lila
package memo

import com.google.common.base.Function
import com.google.common.cache._
import java.util.concurrent.TimeUnit

object Builder {

  /**
   * A caching wrapper for a function (K => V),
   * backed by a Cache from Google Collections.
   */
  def cache[K, V](ttl: Int, f: K ⇒ V): LoadingCache[K, V] =
    CacheBuilder.newBuilder()
      .expireAfterWrite(ttl, TimeUnit.MILLISECONDS)
      .asInstanceOf[CacheBuilder[K, V]]
      .build[K, V](f)

  def expiry[K, V](ttl: Int): Cache[K, V] =
    CacheBuilder.newBuilder()
      .expireAfterWrite(ttl, TimeUnit.MILLISECONDS)
      .asInstanceOf[CacheBuilder[K, V]]
      .build[K, V]

  implicit def functionToGoogleFunction[T, R](f: T ⇒ R): Function[T, R] =
    new Function[T, R] {
      def apply(p1: T) = f(p1)
    }

  implicit def functionToGoogleCacheLoader[T, R](f: T ⇒ R): CacheLoader[T, R] =
    new CacheLoader[T, R] {
      def load(p1: T) = f(p1)
    }
}
