package lila.memo

object OnceEvery:

  def apply[K](ttl: FiniteDuration): K => Boolean =

    val cache = ExpireSetMemo[K](ttl)

    key =>
      val isNew = !cache.get(key)
      if isNew then cache.put(key)
      isNew

  def hashCode[A](ttl: FiniteDuration): A => Boolean =

    val cache = HashCodeExpireSetMemo[A](ttl)

    key =>
      val isNew = !cache.get(key)
      if isNew then cache.put(key)
      isNew
