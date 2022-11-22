package lila.memo

import scala.concurrent.duration.FiniteDuration

object OnceEvery:

  def apply[K](ttl: FiniteDuration)(using bts: BasicallyTheSame[K, String]): K => Boolean =

    val cache = new ExpireSetMemo[String](ttl)

    key => {
      val isNew = !cache.get(bts(key))
      if (isNew) cache.put(bts(key))
      isNew
    }

  def hashCode[A](ttl: FiniteDuration): A => Boolean =

    val cache = new HashCodeExpireSetMemo[A](ttl)

    key => {
      val isNew = !cache.get(key)
      if (isNew) cache.put(key)
      isNew
    }
