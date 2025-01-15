package lila.storm

import com.github.blemale.scaffeine.LoadingCache
import com.roundeights.hasher.Algo
import scalalib.ThreadLocalRandom

import lila.common.Uptime
import lila.core.config.Secret
import lila.memo.CacheApi

final class StormSign(secret: Secret)(using Executor):

  private val store: LoadingCache[UserId, String] =
    CacheApi.scaffeine
      .expireAfterAccess(24.hours)
      .build(_ => ThreadLocalRandom.nextString(12))

  private val signer = Algo.hmac(secret.value)

  def getPrev(user: User): String = store.get(user.id)

  def check(user: User, signed: String): Boolean = signed != "undefined" && {
    val correct =
      !Uptime.startedSinceMinutes(5) || {
        signer.sha1(store.get(user.id)).hash_=(signed)
      }
    if correct then store.put(user.id, signed)
    correct
  }
