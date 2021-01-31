package lila.memo

import scala.concurrent.duration.FiniteDuration

object OnceEvery {

  private type Key = String

  def apply(ttl: FiniteDuration): Key => Boolean = {

    val cache = new ExpireSetMemo(ttl)

    key => {
      val isNew = !cache.get(key)
      if (isNew) cache.put(key)
      isNew
    }
  }
}
