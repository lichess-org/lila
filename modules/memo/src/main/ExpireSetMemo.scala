package lila.memo

import com.github.blemale.scaffeine.Cache

final class ExpireSetMemo[K](ttl: FiniteDuration):

  private val cache: Cache[K, Boolean] = CacheApi.scaffeineNoScheduler
    .expireAfterWrite(ttl)
    .build[K, Boolean]()

  def get(key: K): Boolean = cache.underlying.getIfPresent(key) == true

  def intersect(keys: Iterable[K]): Set[K] =
    keys.nonEmpty so {
      val res = cache getAllPresent keys
      keys filter res.contains toSet
    }

  def put(key: K) = cache.put(key, true)

  def putAll(keys: Iterable[K]) = cache putAll keys.view.map(k => k -> true).toMap

  def remove(key: K) = cache invalidate key

  def removeAll(keys: Iterable[K]) = cache invalidateAll keys

  def keys: Iterable[K] = cache.asMap().keys

  def keySet: Set[K] = keys.toSet

  def count = cache.estimatedSize().toInt

final class HashCodeExpireSetMemo[A](ttl: FiniteDuration):

  private val cache: Cache[Int, Boolean] = CacheApi.scaffeineNoScheduler
    .expireAfterWrite(ttl)
    .build[Int, Boolean]()

  def get(key: A): Boolean = cache.underlying.getIfPresent(key.hashCode) == true

  def put(key: A) = cache.put(key.hashCode, true)

  def remove(key: A) = cache invalidate key.hashCode
