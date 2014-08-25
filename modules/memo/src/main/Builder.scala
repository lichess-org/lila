package lila.memo

import java.util.concurrent.TimeUnit
import scala.concurrent.duration.Duration

import com.google.common.base.Function
import com.google.common.cache._

object Builder {

  private implicit def durationToMillis(d: Duration): Long = d.toMillis

  /**
   * A caching wrapper for a function (K => V),
   * backed by a Cache from Google Collections.
   */
  def cache[K, V](ttl: Duration, f: K => V): LoadingCache[K, V] =
    cacheBuilder[K, V](ttl)
      .build[K, V](f)

  def expiry[K, V](ttl: Duration): Cache[K, V] =
    cacheBuilder[K, V](ttl).build[K, V]

  def size[K, V](max: Int): Cache[K, V] =
    CacheBuilder.newBuilder()
      .maximumSize(max)
      .asInstanceOf[CacheBuilder[K, V]]
      .build[K, V]

  private def cacheBuilder[K, V](ttl: Duration): CacheBuilder[K, V] =
    CacheBuilder.newBuilder()
      .expireAfterWrite(ttl, TimeUnit.MILLISECONDS)
      .asInstanceOf[CacheBuilder[K, V]]

  implicit def functionToRemovalListener[K, V](f: (K, V) => Unit): RemovalListener[K, V] =
    new RemovalListener[K, V] {
      def onRemoval(notification: RemovalNotification[K, V]) =
        f(notification.getKey, notification.getValue)
    }

  implicit def functionToGoogleFunction[T, R](f: T => R): Function[T, R] =
    new Function[T, R] {
      def apply(p1: T) = f(p1)
    }

  implicit def functionToGoogleCacheLoader[T, R](f: T => R): CacheLoader[T, R] =
    new CacheLoader[T, R] {
      def load(p1: T) = f(p1)
    }
}
