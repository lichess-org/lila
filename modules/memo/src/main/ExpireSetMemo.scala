package lila.memo

import com.github.blemale.scaffeine.Cache
import alleycats.Zero
import scala.annotation.nowarn
import scala.concurrent.duration.FiniteDuration

final class ExpireSetMemo[K](ttl: FiniteDuration)(using ev: K =:= String):

  private val cache: Cache[String, Boolean] = CacheApi.scaffeineNoScheduler
    .expireAfterWrite(ttl)
    .build[String, Boolean]()

  @nowarn def get(key: K): Boolean = cache.underlying.getIfPresent(key) == true

  def intersect(keys: Iterable[K]): Set[K] =
    keys.nonEmpty ?? {
      val res = cache getAllPresent keys.map(ev.apply)
      keys filter res.contains toSet
    }

  def put(key: K) = cache.put(key, true)

  def putAll(keys: Iterable[K]) = cache putAll keys.view.map(k => ev(k) -> true).toMap

  def remove(key: K) = cache invalidate key

  def removeAll(keys: Iterable[K]) = cache invalidateAll keys.map(ev.apply)

  def keys: Iterable[K] = cache.asMap().keys map ev.flip.apply

  def keySet: Set[K] = keys.toSet

  def count = cache.estimatedSize().toInt

final class HashCodeExpireSetMemo[A](ttl: FiniteDuration):

  private val cache: Cache[Int, Boolean] = CacheApi.scaffeineNoScheduler
    .expireAfterWrite(ttl)
    .build[Int, Boolean]()

  @nowarn def get(key: A): Boolean = cache.underlying.getIfPresent(key.hashCode) == true

  def put(key: A) = cache.put(key.hashCode, true)

  def remove(key: A) = cache invalidate key.hashCode
