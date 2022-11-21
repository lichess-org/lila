package lila.memo

import scala.concurrent.duration.FiniteDuration

object OnceEvery:

  def apply[K <: String](ttl: FiniteDuration): K => Boolean =

    val cache = new ExpireSetMemo[K](ttl)

    key => {
      val isNew = !cache.get(key)
      if (isNew) cache.put(key)
      isNew
    }

  def hashCode[A](ttl: FiniteDuration): A => Boolean =

    val cache = new HashCodeExpireSetMemo[A](ttl)

    key => {
      val isNew = !cache.get(key)
      if (isNew) cache.put(key)
      isNew
    }
